package com.wooduan.lightmc.sample;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

public class LogManager {

	

	public static void initConfig(String configFile) {
		LoggerContext context = (LoggerContext) LoggerFactory
				.getILoggerFactory();

		try {
			JoranConfigurator configurator = new JoranConfigurator();
			configurator.setContext(context);
			context.reset();
			configurator.doConfigure(configFile);
		} catch (JoranException je) {
			System.err.println("config logback failed");
		}
		StatusPrinter.printInCaseOfErrorsOrWarnings(context);

	}
}
