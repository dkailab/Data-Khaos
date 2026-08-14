package com.datakhaos.pipeline.engine;

import cn.hutool.core.util.StrUtil;
import com.datakhaos.common.exception.BusinessException;
import com.datakhaos.common.util.EncryptUtil;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * 引擎共用工具：数据源连接信息获取 / 密码解密 / JDBC URL 构造。
 * 供 DataX / SeaTunnel 引擎生成运行配置时复用。
 */
public final class EngineUtils {

    private EngineUtils() {
    }

    /** 从 meta_datasource 读取某数据源连接信息 */
    public static Map<String, Object> getDs(JdbcTemplate jdbcTemplate, String dsId) {
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT ds_type, host, port, database_name, username, password FROM meta_datasource WHERE id = ?", dsId);
        if (list.isEmpty()) {
            throw new BusinessException("数据源不存在: " + dsId);
        }
        return list.get(0);
    }

    public static String decryptPwd(String pwd, String aesKey) {
        return StrUtil.isBlank(pwd) ? "" : EncryptUtil.decrypt(pwd, aesKey);
    }

    public static String jdbcUrl(Map<String, Object> ds) {
        String host = String.valueOf(ds.get("host"));
        int port = ((Number) ds.get("port")).intValue();
        String db = String.valueOf(ds.get("database_name"));
        return "jdbc:mysql://" + host + ":" + port + "/" + db
                + "?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";
    }

    public static void checkMysql(Map<String, Object> source, Map<String, Object> target) {
        String st = String.valueOf(source.get("ds_type"));
        String tt = String.valueOf(target.get("ds_type"));
        if (!"MYSQL".equalsIgnoreCase(st) || !"MYSQL".equalsIgnoreCase(tt)) {
            throw new BusinessException("当前引擎仅支持 MySQL 源/目标（" + st + "→" + tt + "），其他类型请扩展引擎适配器");
        }
    }

    /** 打开一个 JDBC 连接（用于校验连通性） */
    public static Connection open(Map<String, Object> ds, String aesKey) throws SQLException {
        return DriverManager.getConnection(jdbcUrl(ds),
                String.valueOf(ds.get("username")), decryptPwd(String.valueOf(ds.get("password")), aesKey));
    }

    /** 从任务 config(JSON) 中读取超时秒数（缺省 0=使用引擎默认） */
    public static long configTimeout(String config) {
        if (StrUtil.isBlank(config)) {
            return 0;
        }
        try {
            Number n = cn.hutool.json.JSONUtil.parseObj(config).getLong("timeoutSeconds");
            return n == null ? 0 : n.longValue();
        } catch (Exception e) {
            return 0;
        }
    }
}