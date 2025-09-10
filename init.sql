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