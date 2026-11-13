<?php

if (!defined('B_PROLOG_INCLUDED') || B_PROLOG_INCLUDED !== true) {
    die();
}

$arComponentParameters = [
    'PARAMETERS' => [
        'IBLOCK_ID' => [
            'PARENT' => 'BASE',
            'NAME' => Loc::getMessage('PARAM_IBLOCK_ID'),
            'TYPE' => 'NUMBER',
            'REQUIRED' => 'Y',
        ],
    ],
];
