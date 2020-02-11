package com.bolyartech.db2source.cli

import com.bolyartech.db2source.*
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Options
import java.io.*
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
        println("ERROR: Cannot find configuration file: $confFile")
    }

    println("Will try to load configuraton from $confFile")

    val prop = Properties()
    try {
        val `is`: InputStream = BufferedInputStream(FileInputStream(confFile))
        prop.load(`is`)
        `is`.close()
    } catch (e: IOException) {
        println("Cannot load config file. ${e.message}")
        exitProcess(2)
    }

    val configLoader = ConfigDataLoader()
    val config = configLoader.load(prop)
    println("Configuration loaded successfully")

    // TODO load drivers depending on conf
    Class.forName("com.mysql.jdbc.Driver")
    Class.forName("org.postgresql.Driver")

    val db2source = Db2Source()
    val rez = db2source.generate(config)
    when (rez) {
        is GenerationResultOk -> println("Generation SUCCESS")
        is GenerationResultErrorCannotConnectDb -> println("Cannot connect to DB: ${rez.reason}")
        is GenerationResultError -> println("Error: ${rez.reason}")
    }

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

        val tc = TableConfig(sourceTable, className, dir)

        return ConfigData(dsn, username, password, schema, listOf(tc))
    }


}
