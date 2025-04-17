{
    "targets": [
        {
            "target_name": "mousehook",
            "sources": ["src/mousehook.cpp"],
            "include_dirs": [
                "<!(node -p \"require('node-addon-api').include\")",
                "<!(pwd)/node_modules/node-addon-api",
            ],
            "defines": ["NAPI_DISABLE_CPP_EXCEPTIONS"],
            "cflags_cc": ["-std=c++17"],
        }
    ]
}
