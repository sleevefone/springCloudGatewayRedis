```text

curl -X POST http://localhost:8888/admin/routes \
-H "Content-Type: application/json" \
-d '{
    "id": "custom_filter_test",
    "uri": "http://httpbin.org",
    "predicates": [
        {
            "name": "Path",
            "args": {
                "_genkey_0": "/custom/**"
            }
        }
    ],
    "filters": [
        {
            "name": "StripPrefix",
            "args": {
                "_genkey_0": "1"
            }
        },
        {
            "name": "AddTimestamp"
        }
    ]
}'
```