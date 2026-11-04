package com.github.saundefined.bitrix_idea.templates.d7

enum class D7ClassType(val displayName: String) {
    CONTROLLER("Контроллер"),
    AJAX_CONTROLLER("AJAX-контроллер"),
    EVENT_HANDLER("Обработчик события"),
    AGENT("Агент"),
    CONSOLE("Консольная команда"),
    SERVICE("Сервис"),
}

object BitrixD7ClassGenerator {
    fun generate(type: D7ClassType, namespace: String, className: String): String {
        val body = when (type) {
            D7ClassType.CONTROLLER -> """
                |class $className extends \Bitrix\Main\Engine\Controller
                |{
                |    public function configureActions(): array
                |    {
                |        return [];
                |    }
                |}
                |""".trimMargin()

            D7ClassType.AJAX_CONTROLLER -> """
                |class $className extends \Bitrix\Main\Engine\Controller
                |{
                |    public function configureActions(): array
                |    {
                |        return [
                |            'list' => [
                |                'prefilters' => [],
                |            ],
                |        ];
                |    }
                |
                |    public function listAction(): array
                |    {
                |        return [];
                |    }
                |}
                |""".trimMargin()

            D7ClassType.EVENT_HANDLER -> """
                |class $className
                |{
                |    public static function handle(\Bitrix\Main\Event ${'$'}event): void
                |    {
                |    }
                |}
                |""".trimMargin()

            D7ClassType.AGENT -> """
                |class $className
                |{
                |    public static function run(): void
                |    {
                |    }
                |}
                |""".trimMargin()

            D7ClassType.CONSOLE -> """
                |use Symfony\Component\Console\Input\InputInterface;
                |use Symfony\Component\Console\Output\OutputInterface;
                |
                |class $className extends \Symfony\Component\Console\Command\Command
                |{
                |    protected function configure(): void
                |    {
                |        ${'$'}this->setDescription('Описание команды.');
                |    }
                |
                |    protected function execute(InputInterface ${'$'}input, OutputInterface ${'$'}output): int
                |    {
                |        return self::SUCCESS;
                |    }
                |}
                |""".trimMargin()

            D7ClassType.SERVICE -> """
                |class $className
                |{
                |}
                |""".trimMargin()
        }

        return buildString {
            appendLine("<?php")
            appendLine()
            if (namespace.isNotBlank()) {
                appendLine("namespace $namespace;")
                appendLine()
            }
            append(body.trimEnd())
            appendLine()
        }
    }
}
