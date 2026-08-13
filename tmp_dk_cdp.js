const { spawn } = require('child_process');
const http = require('http');
const fs = require('fs');

const CHROME = '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome';
const PORT = 9223;
const OUT = '/tmp/dk_full';
const WS = require('ws');

function getJson(url) {
  return new Promise((res, rej) => {
    http.get(url, (r) => {
      let d = '';
      r.on('data', (c) => (d += c));
      r.on('end', () => res(JSON.parse(d)));
    }).on('error', rej);
  });
}

function wsSend(ws, id, method, params) {
  return new Promise((res, rej) => {
    const handler = (ev) => {
      const m = JSON.parse(ev.data.toString());
      if (m.id === id) {
        ws.removeListener('message', handler);
        if (m.error) rej(new Error(m.error.message));
        else res(m.result);
      }
    };
    ws.on('message', handler);
    ws.send(JSON.stringify({ id, method, params }));
  });
}

async function main() {
  const chrome = spawn(CHROME, [
    '--headless=new', '--disable-gpu', '--no-sandbox',
    `--remote-debugging-port=${PORT}`,
    '--user-data-dir=/tmp/dkchrome_cdp',
    '--hide-scrollbars',
    'about:blank',
  ], { stdio: 'ignore' });

  let target;
  for (let i = 0; i < 40; i++) {
    try {
      target = await getJson(`http://127.0.0.1:${PORT}/json`);
      if (target.length) break;
    } catch (e) {}
    await new Promise(r => setTimeout(r, 300));
  }
  if (!target || !target.length) { console.error('no target'); chrome.kill(); return; }

  const page = target.find(t => t.type === 'page');
  const ws = new WS(page.webSocketDebuggerUrl);
  await new Promise(r => ws.on('open', r));

  let seq = 0;
  const send = (m, p) => wsSend(ws, ++seq, m, p);

  await send('Page.enable', {});
  await send('Runtime.enable', {});
  await send('Emulation.setDeviceMetricsOverride', { width: 1600, height: 900, deviceScaleFactor: 1, mobile: false });

  await send('Page.navigate', { url: 'http://localhost:5173/_boot.html' });
  await new Promise(r => setTimeout(r, 9000));

  const evalRes = await send('Runtime.evaluate', {
    expression: `(() => {
      const cells = [...document.querySelectorAll('.board-cell')];
      const items = [...document.querySelectorAll('.board-grid-item')];
      const rect = (el) => { const r = el.getBoundingClientRect(); return {t:+r.top.toFixed(1),b:+r.bottom.toFixed(1),l:+r.left.toFixed(1),rgt:+r.right.toFixed(1),w:+r.width.toFixed(1),h:+r.height.toFixed(1)}; };
      const info = {
        url: location.href,
        title: document.title,
        bodyH: document.body.scrollHeight,
        cells: cells.map(c => ({ name: (c.querySelector('.board-name')||{}).textContent, rect: rect(c) })),
        items: items.map(i => ({ title: (i.querySelector('.item-title')||{}).textContent, rect: rect(i) })),
      };
      return JSON.stringify(info);
    })()`,
    returnByValue: true,
  });
  fs.writeFileSync('/tmp/dk_layout.json', evalRes.result.value);
  console.log('LAYOUT_SAVED');

  const layout = JSON.parse(evalRes.result.value);
  const fullH = Math.max(layout.bodyH, 6000);
  await send('Emulation.setDeviceMetricsOverride', { width: 1600, height: fullH, deviceScaleFactor: 1, mobile: false });
  await new Promise(r => setTimeout(r, 2000));
  const snap = await send('Page.captureScreenshot', { format: 'png', captureBeyondViewport: true });
  fs.writeFileSync(OUT + '.png', Buffer.from(snap.data, 'base64'));
  console.log('SHOT_SAVED', fullH);

  ws.close();
  chrome.kill();
}

main().catch(e => { console.error(e); process.exit(1); });