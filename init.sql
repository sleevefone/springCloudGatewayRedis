

USE `gateway_db`;


DROP TABLE IF EXISTS `api_clients`;
CREATE TABLE `api_clients` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `app_key` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `enabled` bit(1) NOT NULL,
  `secret_key` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK4n6iknay8u34tt5ty3dpsyl9x` (`app_key`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `gateway_routes` (
  `id` varchar(100) NOT NULL,
  `uri` text NOT NULL,
  `predicates` text NOT NULL,
  `filters` text,
  `route_order` int NOT NULL DEFAULT '0',
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `predicate_description` varchar(255) DEFAULT NULL,
  `filter_description` varchar(255) DEFAULT NULL,
  `creator` varchar(255) NOT NULL DEFAULT 'system',
  `updater` varchar(255) NOT NULL DEFAULT 'system',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
INSERT INTO `gateway_routes` VALUES ('1111','https://httpbin.org12241error','[{\"name\":\"Path\",\"args\":{\"_genkey_0\":\"/configurable/**\"}}]','[{\"name\":\"StripPrefix\",\"args\":{}}]',0,0,NULL,NULL,'admin','admin','2025-09-07 02:28:12','2025-09-08 07:05:48'),('caching-route','http://your-backend-service:8081','[{\"name\":\"Path\",\"args\":{\"patterns\":\"/api/**\"}}]','[{\"name\":\"CacheResponse\",\"args\":{\"ttlMinutes\":\"10\"}}]',0,1,NULL,NULL,'admin','admin','2025-09-08 06:58:55','2025-09-08 07:05:48'),('configurable_filter_test','https://httpbin.org12241','[{\"name\":\"Path\",\"args\":{\"_genkey_0\":\"/configurable/**\"}}]','[{\"name\":\"StripPrefix\",\"args\":{\"_genkey_0\":\"1\"}},{\"name\":\"AddTimestamp\",\"args\":{\"headerName\":\"v666Timestamp\"}},{\"name\":\"AddTimestamp\",\"args\":{\"testHeader\":\"testValue\"}}]',0,0,NULL,NULL,'admin','admin','2025-09-07 02:28:12','2025-09-08 07:05:48'),('dynamic-unit-routing-rule','http://localhost:8081','[{\"name\":\"Path\",\"args\":{\"patterns\":\"/api/**\"}}]','[{\"name\":\"StripPrefix\",\"args\":{\"_genkey_0\":\"1\"}},{\"name\":\"UnitSelection\",\"args\":{}}]',100,0,NULL,NULL,'admin','admin','2025-09-07 02:28:12','2025-09-08 07:05:48'),('my-service-canary-route','http://localhost:8888','[{\"name\":\"Path\",\"args\":{\"patterns\":\"/api/**\"}}]','[{\"name\":\"StripPrefix\",\"args\":{\"_genkey_0\":\"1\"}},{\"name\":\"CanaryRouting\",\"args\":{\"headerName\":\"X-Version\",\"headerValue\":\"v2\",\"canaryUri\":\"http://my-canary-service:8080\"}}]',10,0,NULL,NULL,'admin','admin','2025-09-07 22:41:49','2025-09-08 07:05:48'),('v3','https://httpbin.org','[{\"name\":\"Path\",\"args\":{\"patterns\":\"/example/**\"}}]','[{\"name\":\"StripPrefix\",\"args\":{\"_genkey_0\":\"1\"}},{\"name\":\"AddTimestamp\",\"args\":{\"headerName\":\"v77Timestamp\"}}]',1,0,NULL,NULL,'admin','admin','2025-09-07 02:28:12','2025-09-08 07:05:48'),('whiteList_rule','http://localhost:9999','[{\"name\":\"Path\",\"args\":{\"patterns\":\"/api/**\"}}]','[{\"name\":\"IpWhiteBlackList\",\"args\":{\"mode\":\"whitelist\",\"whiteListKey\":\"gateway:ip:whitelist\",\"blackListKey\":\"gateway:ip:blacklist\"}}]',0,0,NULL,NULL,'admin','admin','2025-09-07 22:21:59','2025-09-08 07:05:48');
