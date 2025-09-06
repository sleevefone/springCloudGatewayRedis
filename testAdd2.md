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



总之IDEA生成的注解处理器配置总是会导致Lombok无法正常工作，你可以尝试将注解处理器配置移动到Default或者Maven default来解决。

https://zhuanlan.zhihu.com/p/28435303285

