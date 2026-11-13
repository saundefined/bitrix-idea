<?php

return [
    'controllers' => [
        'value' => [
            'namespaces' => [
                '\\Vendor\\Module\\Controller' => 'api',
            ],
        ],
    ],
    'events' => [
        'value' => [
            'main' => [
                'OnBeforeProlog' => [
                    ['\\Vendor\\Module\\Handlers', 'onBeforeProlog'],
                ],
            ],
        ],
    ],
];
