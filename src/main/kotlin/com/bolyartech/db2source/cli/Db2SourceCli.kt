package com.bolyartech.db2source.cli

import com.andreapivetta.kolor.green
import com.andreapivetta.kolor.red
import com.bolyartech.db2source.*
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Options
import java.io.*
import java.lang.System.exit
import java.util.*
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val DEFAULT_CONFIG_FILENAME = "db2source.conf"
    val DEFAULT_CONFIG_PATH = "conf"

    val cmd: CommandLine = parseCommandLine(args)
    var configPath = cmd.getOptionValue("config-file")
    if (configPath == null) {
        configPath = System.getProperty("user.dir") + File.separator + DEFAULT_CONFIG_PATH + File.separator +
                DEFAULT_CONFIG_FILENAME

        println("No configuration file specified (use -c option). Will try $configPath")
    }

    val confFile = File(configPath)
    if (!confFile.exists()) {
        errorMessage("Cannot find configuration file: $confFile")
    }

    println("Will try to load configuraton from $confFile")

    val prop = Properties()
    try {
        val `is`: InputStream = BufferedInputStream(FileInputStream(confFile))
        prop.load(`is`)
        `is`.close()
    } catch (e: IOException) {
        errorMessage("Cannot load config file. ${e.message}")
        exitProcess(2)
    }

    val configLoader = ConfigDataLoader()
    val config = configLoader.load(prop)
    println("Configuration loaded successfully")

    // TODO load drivers depending on conf
//    Class.forName("com.mysql.jdbc.Driver")
    Class.forName("org.postgresql.Driver")

    val db2source = Db2Source()
    try {
        val oc = db2source.generate(config)
        when (oc) {
            is GenerationResultOk -> {
                println("+++".green())
                println("\uD83D\uDE00 Successfully generated: ${oc.generatedFileName}".green())
                println("+++".green())
            }

            is GenerationResultErrorCannotConnectDb -> errorMessage("Cannot connect to DB: ${oc.reason}")
            is GenerationResultError -> errorMessage(oc.reason)
            is GenerationResultErrorTableNotFound -> errorMessage("DB table '${oc.table}' not found")
        }
    } catch (e: Exception) {
        errorMessage("Error: ${e.message}".red())
        exit(1)
    }
}

fun errorMessage(msg: String) {
    println("+++".red())
    println("\uD83D\uDE31 ERROR: ${msg}".red())
    println("+++".red())
}


fun parseCommandLine(args: Array<String>): CommandLine {
    val argsParser = DefaultParser()

    return argsParser.parse(createCliArgOptions(), args)
}

fun createCliArgOptions(): Options {
    val cliOptions = Options()
    cliOptions.addOption("c", "config-file", true, "path to configuration file")
    return cliOptions
}


class ConfigDataLoader {
    private val KEY_DSN = "db_dsn"
    private val KEY_USERNAME = "db_username"
    private val KEY_PASSWORD = "db_password"
    private val KEY_SCHEMA = "db_schema"
    private val KEY_SOURCE_TABLE = "source_table"
    private val KEY_DESTINATION_CLASS_NAME = "destination_class_name"
    private val KEY_DESTINATION_DIR = "destination_dir"
    private val KEY_ADD_PAGINATION_METHODS = "add_pagination_methods"
    private val KEY_CREATE_VALUE_CLASS_FOR_ID = "create_value_class_for_id"
    private val KEY_ADD_DEPENDENCY_INJECTION_CODE = "add_dependency_injection_code"
    private val KEY_ADD_LOCK_METHOD = "add_lock_method"

    fun load(prop: Properties): ConfigData {
        val dsn = prop.getProperty(KEY_DSN)
        if (dsn == null) {
            println("Missing $KEY_DSN.")
            exitProcess(3)
        }

        val username = prop.getProperty(KEY_USERNAME)
        if (username == null) {
            println("Missing $KEY_USERNAME.")
            exitProcess(3)
        }

        val password = prop.getProperty(KEY_PASSWORD)
        if (password == null) {
            println("Missing $KEY_PASSWORD.")
            exitProcess(3)
        }

        val schema = prop.getProperty(KEY_SCHEMA)
        if (schema == null) {
            println("Missing $KEY_SCHEMA.")
            exitProcess(3)
        }

        val sourceTable = prop.getProperty(KEY_SOURCE_TABLE)
        if (sourceTable == null) {
            println("Missing $KEY_SOURCE_TABLE.")
            exitProcess(3)
        }

        val className = prop.getProperty(KEY_DESTINATION_CLASS_NAME)
        if (className == null) {
            println("Missing $KEY_DESTINATION_CLASS_NAME.")
            exitProcess(3)
        }

        val dir = prop.getProperty(KEY_DESTINATION_DIR)
        if (dir == null) {
            println("Missing $KEY_DESTINATION_DIR.")
            exitProcess(3)
        }

        val ddir = File(dir)
        if (!ddir.exists()) {
            println("Destination dir does not exist: $dir")
            exitProcess(4)
        }

        val addPaginationMethods = prop.getProperty(KEY_ADD_PAGINATION_METHODS)
        val addPaginationMethodsFinal: Boolean = addPaginationMethods.equals("1") || addPaginationMethods.lowercase(Locale.getDefault()).equals("yes") ||
                addPaginationMethods.lowercase(Locale.getDefault()).equals("y")

        val createValueClassForId = prop.getProperty(KEY_CREATE_VALUE_CLASS_FOR_ID)
        val createValueClassForIdFinal: Boolean = createValueClassForId.equals("1") || createValueClassForId.lowercase(Locale.getDefault()).equals("yes") ||
                createValueClassForId.lowercase(Locale.getDefault()).equals("y")

        val addDiCode = prop.getProperty(KEY_ADD_DEPENDENCY_INJECTION_CODE)
        val addDiCodeFinal: Boolean = addDiCode.equals("1") || addDiCode.lowercase(Locale.getDefault()).equals("yes") ||
                addDiCode.lowercase(Locale.getDefault()).equals("y")

        val addLockMethod = prop.getProperty(KEY_ADD_LOCK_METHOD)
        val addLockMethodFinal: Boolean = addLockMethod.equals("1") || addLockMethod.lowercase(Locale.getDefault()).equals("yes") ||
                addLockMethod.lowercase(Locale.getDefault()).equals("y")

        val tc = TableConfig(sourceTable, className, dir)

        return ConfigData(dsn, username, password, schema, listOf(tc), addPaginationMethodsFinal, createValueClassForIdFinal, addDiCodeFinal, addLockMethodFinal)
    }


}
