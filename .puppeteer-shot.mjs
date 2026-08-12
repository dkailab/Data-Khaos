import puppeteer from '/tmp/pptr/node_modules/puppeteer-core/lib/esm/puppeteer/puppeteer-core.js'

const CHROME = '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome'
const base = 'http://localhost:5173'
const dashId = '2087425001321390081'

const login = await fetch('http://localhost:8080/api/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username: 'admin', password: 'password' }),
})
const token = (await login.json()).data.token

const browser = await puppeteer.launch({
  executablePath: CHROME, headless: 'new',
  args: ['--no-sandbox', '--force-device-scale-factor=1'],
  defaultViewport: { width: 1440, height: 900 },
})
const page = await browser.newPage()
await page.goto(base + '/login', { waitUntil: 'networkidle2' })
await page.evaluate((t) => localStorage.setItem('dk_token', t), token)
await page.goto(`${base}/visual/dashboard/edit/${dashId}`, { waitUntil: 'networkidle2', timeout: 60000 })
await new Promise((r) => setTimeout(r, 4000))

const info = await page.evaluate(() => {
  const out = { cells: [] }
  document.querySelectorAll('.board-cell').forEach((cell) => {
    const r = cell.getBoundingClientRect()
    const body = cell.querySelector('.board-body')
    const bodyR = body ? body.getBoundingClientRect() : null
    const cs = getComputedStyle(cell)
    out.cells.push({
      name: cell.querySelector('.board-name')?.textContent || '',
      cellH: Math.round(r.height),
      bodyH: bodyR ? Math.round(bodyR.height) : null,
      bodyTop: bodyR ? Math.round(bodyR.top) : null,
      cellTop: Math.round(r.top),
      alignSelf: cs.alignSelf,
      gridRowEnd: cs.gridRowEnd,
      minHeight: cs.minHeight,
    })
  })
  return out
})
console.log(JSON.stringify(info, null, 2))

await new Promise((r) => setTimeout(r, 2000))
await page.screenshot({ path: '/tmp/pptr/dashboard_viewport.png' })
await browser.close()