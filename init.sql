CREATE TABLE `gateway_routes` (
  `id` varchar(100) NOT NULL,
  `uri` text NOT NULL,
  `predicates` text NOT NULL,
  `filters` text,
  `route_order` int NOT NULL DEFAULT '0',
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB


    <script src="./vender/react.development.js"></script>
    <script src="./vender/react-dom.development.js"></script>

    <!-- Babel to transpile JSX in the browser -->
    <script src="./vender/babel.min.js"></script>

    <!-- Axios for API calls -->
    <script src="./vender/axios.min.js"></script>


[ { "predicate": "Paths: [/configurable/**], match trailing slash: true", "route_id": "1111", "filters": [ "[[StripPrefix parts = 1], order = 1]" ], "uri": "https://httpbin.org12241error:443", "order": 0 }, { "predicate": "Paths: [/api/**], match trailing slash: true", "route_id": "caching-route", "filters": [ "[com.ocft.gateway.openapi.constant.CacheResponseGatewayFilterFactory$$Lambda/0x00007f8001ddbdb0@3038d055, order = 1]" ], "uri": "http://your-backend-service:8081", "order": 0 } ]