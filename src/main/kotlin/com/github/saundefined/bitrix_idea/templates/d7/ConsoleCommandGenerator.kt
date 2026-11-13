package com.github.saundefined.bitrix_idea.templates.d7

/**
 * Генерация консольной команды Bitrix.
 */
object ConsoleCommandGenerator {
    fun generate(
        namespace: String,
        className: String,
        commandName: String,
        description: String,
        arguments: List<String>,
    ): String {
        return buildString {
            appendLine("<?php")
            appendLine()
            if (namespace.isNotBlank()) {
                appendLine("namespace $namespace;")
                appendLine()
            }
            appendLine("use Symfony\\Component\\Console\\Input\\InputArgument;")
            appendLine("use Symfony\\Component\\Console\\Input\\InputInterface;")
            appendLine("use Symfony\\Component\\Console\\Output\\OutputInterface;")
            appendLine()
            appendLine("class $className extends \\Symfony\\Component\\Console\\Command\\Command")
            appendLine("{")
            appendLine("    protected function configure(): void")
            appendLine("    {")
            appendLine("        \$this")
            appendLine("            ->setName('${escape(commandName)}')")
            appendLine("            ->setDescription('${escape(description)}');")
            arguments.forEach { argument ->
                appendLine("        \$this->addArgument('${escape(argument)}', InputArgument::OPTIONAL);")
            }
            appendLine("    }")
            appendLine()
            appendLine("    protected function execute(InputInterface \$input, OutputInterface \$output): int")
            appendLine("    {")
            arguments.forEach { argument ->
                appendLine("        \$output->writeln('${escape(argument)}: ' . \$input->getArgument('${escape(argument)}'));")
            }
            appendLine("        return self::SUCCESS;")
            appendLine("    }")
            appendLine("}")
        }
    }

    private fun escape(value: String): String = value.replace("\\", "\\\\").replace("'", "\\'")
}
