
package org.eclipse.store.demo.bookstore;

/*-
 * #%L
 * EclipseStore BookStore Demo
 * %%
 * Copyright (C) 2023 MicroStream Software
 * %%
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * #L%
 */

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.money.MonetaryAmount;
import javax.money.format.AmountFormatQueryBuilder;
import javax.money.format.MonetaryAmountFormat;
import javax.money.format.MonetaryFormats;

import org.eclipse.serializer.persistence.binary.jdk8.types.BinaryHandlersJDK8;
import org.eclipse.store.demo.bookstore.data.Data;
import org.eclipse.store.demo.bookstore.data.DataMetrics;
import org.eclipse.store.demo.bookstore.data.RandomDataAmount;
import org.eclipse.store.storage.embedded.configuration.types.EmbeddedStorageConfiguration;
import org.eclipse.store.storage.embedded.types.EmbeddedStorageFoundation;
import org.eclipse.store.storage.embedded.types.EmbeddedStorageManager;
import org.javamoney.moneta.RoundedMoney;
import org.javamoney.moneta.format.CurrencyStyle;
import org.rapidpm.dependencies.core.logger.HasLogger;
import org.springframework.boot.SpringApplication;


/**
 * Central singleton, which holds the {@link EmbeddedStorageManager} and the {@link Data} root object.
 * <p>
 * The demo application simulates a worldwide operating book sales company with stores in many countries.
 * <p>
 * Note: If you start the {@link VaadinApplication}, which is a {@link SpringApplication},
 * it is created by the {@link VaadinApplicationConfiguration}.
 *
 * @see #data()
 * @see #storageManager()
 * @see <a href="https://docs.eclipsestore.io/manual/storage/getting-started.html">EclipseStore Reference Manual</a>
 */
public final class BookStoreDemo implements HasLogger
{
	private static BookStoreDemo instance;

	/**
	 * @return the single instance of this class
	 */
	public static BookStoreDemo getInstance()
	{
		return instance;
	}


	/**
	 * {@link CurrencyUnit} for this demo, US Dollar is used as only currency.
	 */
	public static final CurrencyUnit         CURRENCY_UNIT          = Monetary.getCurrency(Locale.US);

	/**
	 * Money format
	 */
	public final static MonetaryAmountFormat MONETARY_AMOUNT_FORMAT = MonetaryFormats.getAmountFormat(
		AmountFormatQueryBuilder.of(Locale.getDefault())
			.set(CurrencyStyle.SYMBOL)
			.build()
	);

	/**
	 * Multiplicant used to calculate retail prices, adds an 11% margin.
	 */
	private final static BigDecimal           RETAIL_MULTIPLICANT    = scale(new BigDecimal(1.11));


	private static BigDecimal scale(final BigDecimal number)
	{
		return number.setScale(2, RoundingMode.HALF_UP);
	}

	/**
	 * Converts a double into a {@link MonetaryAmount}
	 * @param number the number to convert
	 * @return the converted {@link MonetaryAmount}
	 */
	public static MonetaryAmount money(final double number)
	{
		return money(new BigDecimal(number));
	}

	/**
	 * Converts a {@link BigDecimal} into a {@link MonetaryAmount}
	 * @param number the number to convert
	 * @return the converted {@link MonetaryAmount}
	 */
	public static MonetaryAmount money(final BigDecimal number)
	{
		return RoundedMoney.of(scale(number), CURRENCY_UNIT);
	}

	/**
	 * Calculates the retail price based on a purchase price by adding a margin.
	 * @param purchasePrice the purchase price
	 * @return the calculated retail price
	 * @see #RETAIL_MULTIPLICANT
	 */
	public static MonetaryAmount retailPrice(
		final MonetaryAmount purchasePrice
	)
	{
		return money(RETAIL_MULTIPLICANT.multiply(new BigDecimal(purchasePrice.getNumber().doubleValue())));
	}


	private final    RandomDataAmount       initialDataAmount;
	private volatile EmbeddedStorageManager storageManager   ;

	/**
	 * Creates a new demo instance.
	 *
	 * @param initialDataAmount the amount of data which should be generated if the database is empty
	 */
	public BookStoreDemo(final RandomDataAmount initialDataAmount)
	{
		super();
		this.initialDataAmount = initialDataAmount;
		BookStoreDemo.instance = this;
	}

	/**
	 * Gets the lazily initialized {@link EmbeddedStorageManager} used by this demo.
	 * If no storage data is found, a {@link Data} root object is generated randomly,
	 * based on the given {@link RandomDataAmount}.
	 *
	 * @return the EclipseStore {@link EmbeddedStorageManager} used by this demo
	 */
	public EmbeddedStorageManager storageManager()
	{
		/*
		 * Double-checked locking to reduce the overhead of acquiring a lock
		 * by testing the locking criterion.
		 * The field (this.storageManager) has to be volatile.
		 */
		if(this.storageManager == null)
		{
			synchronized(this)
			{
				if(this.storageManager == null)
				{
					this.storageManager = this.createStorageManager();
				}
			}
		}

		return this.storageManager;
	}

	/**
	 * Creates an {@link EmbeddedStorageManager} and initializes random {@link Data} if empty.
	 */
	private EmbeddedStorageManager createStorageManager()
	{
		this.logger().info("Initializing EclipseStore StorageManager");
		
		final EmbeddedStorageFoundation<?> foundation = EmbeddedStorageConfiguration.Builder()
			.setStorageDirectory("data/storage-with-gigamap")
			.setChannelCount(Math.max(
				1, // minimum one channel, if only 1 core is available
				Integer.highestOneBit(Runtime.getRuntime().availableProcessors() - 1)
			))
			.createEmbeddedStorageFoundation();

		foundation.onConnectionFoundation(BinaryHandlersJDK8::registerJDK8TypeHandlers);
		final EmbeddedStorageManager storageManager = foundation.createEmbeddedStorageManager().start();

		if(storageManager.root() == null)
		{
			this.logger().info("No data found, initializing random data");

			final Data data = new Data();
			storageManager.setRoot(data);
			storageManager.storeRoot();
			final DataMetrics metrics = data.populate(
				this.initialDataAmount,
				storageManager
			);

			this.logger().info("Random data generated: " + metrics.toString());
		}

		return storageManager;
	}

	/**
	 * Gets the {@link Data} root object of this demo.
	 * This is the entry point to all of the data used in this application, basically the "database".
	 *
	 * @return the {@link Data} root object of this demo
	 */
	public Data data()
	{
		return (Data)this.storageManager().root();
	}

	/**
	 * Shuts down the {@link EmbeddedStorageManager} of this demo.
	 */
	public synchronized void shutdown()
	{
		if(this.storageManager != null)
		{
			this.storageManager.shutdown();
			this.storageManager = null;
		}
	}

}
