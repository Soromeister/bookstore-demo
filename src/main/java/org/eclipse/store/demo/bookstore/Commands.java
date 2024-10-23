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

import java.time.Year;
import java.util.List;

import org.eclipse.store.demo.bookstore.data.Book;
import org.eclipse.store.demo.bookstore.data.BookSales;
import org.eclipse.store.demo.bookstore.data.Country;
import org.eclipse.store.demo.bookstore.data.Data;
import org.eclipse.store.demo.bookstore.data.Employee;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;


/**
 * Collection of picocli {@link Command}s for the {@link Repl}.
 *
 */
@Command(name = "")
public class Commands
{
	public static CommandLine createCommandLine(final BookStoreDemo bookStoreDemo)
	{
		final CommandLine cli = new CommandLine(new Commands());
		cli.addSubcommand(new Books(bookStoreDemo));
		cli.addSubcommand(new BestSellerList(bookStoreDemo));
		cli.addSubcommand(new PurchasesOfForeigners(bookStoreDemo));
		cli.addSubcommand(new EmployeeOfTheYear(bookStoreDemo));
		cli.addSubcommand(new Exit(bookStoreDemo));
		return cli;
	}

	static abstract class Abstract implements Runnable
	{
		final BookStoreDemo bookStoreDemo;

		Abstract(final BookStoreDemo bookStoreDemo)
		{
			this.bookStoreDemo = bookStoreDemo;
		}

		Data data()
		{
			return this.bookStoreDemo.data();
		}

		Country searchCountry(final String countryCode)
		{
			return this.data().shops().countryByCode(countryCode);
		}

	}

	@Command(
		name = "books",
		description = "Prints the books found by the given query.",
		mixinStandardHelpOptions = true
	)
	static class Books extends Abstract
	{
		@Option(
			names = {"--query", "-q"},
			description = "the title query",
			required = true
		)
		String query;

		Books(final BookStoreDemo bookStoreDemo)
		{
			super(bookStoreDemo);
		}

		@Override
		public void run()
		{
			final List<Book> books = this.data().books().searchByTitle(this.query);
			if(books.isEmpty())
			{
				System.out.println("No books found");
			}
			else
			{
				System.out.println(books.size() + " books found:");
				books.stream().forEach(book ->
				{
					System.out.println(book.title() + "; by " + book.author().name());
				});
			}
		}
	}

	@Command(
		name = "bestSellerList",
		aliases = {"bsl"},
		description = "Prints the top ten best selling books of the given year.",
		mixinStandardHelpOptions = true
	)
	static class BestSellerList extends Abstract
	{
		@Option(
			names = {"--year", "-y"},
			description = "the year for the best seller list",
			required = false,
			defaultValue = "0"
		)
		int    year;

		@Option(
			names = {"--country", "-c"},
			description = " ISO 3166 alpha-2 country code",
			required = false,
			defaultValue = ""
		)
		String country;

		BestSellerList(final BookStoreDemo bookStoreDemo)
		{
			super(bookStoreDemo);
		}

		@Override
		public void run()
		{
			final int year = this.year == 0
				? Year.now().getValue()
				: this.year;

			if(this.country.isEmpty())
			{
				final List<BookSales> bestSellerList = this.data().purchases().bestSellerList(year);
				if(bestSellerList.isEmpty())
				{
					System.out.println("No books sold in " + year);
				}
				else
				{
					System.out.println("Best selling books in " + year);
					bestSellerList.stream().limit(10).forEach(sales ->
					{
						System.out.println(sales.amount() + " "
							+ sales.book().title() + "; by "
							+ sales.book().author().name()
						);
					});
				}
			}
			else
			{
				final Country country = this.searchCountry(this.country);
				if(country == null)
				{
					System.out.println("Country not found");
				}
				else
				{
					final List<BookSales> bestSellerList = this.data().purchases().bestSellerList(year, country);
					if(bestSellerList.isEmpty())
					{
						System.out.println("No books sold in " + country.name() + " in " + year);
					}
					else
					{
						System.out.println("Best selling books in " + country.name() + " in " + year);
						bestSellerList.stream().limit(10).forEach(sales ->
						{
							System.out.println(sales.amount() + " "
								+ sales.book().title() + "; by "
								+ sales.book().author().name()
							);
						});
					}
				}
			}
		}
	}

	@Command(
		name = "purchasesOfForeigners",
		aliases = {"pof"},
		description = "Prints the count of purchases of foreigners.",
		mixinStandardHelpOptions = true
	)
	static class PurchasesOfForeigners extends Abstract
	{
		@Option(
			names = {"--year", "-y"},
			description = "the year for the calculation",
			required = false,
			defaultValue = "0"
		)
		int    year;

		@Option(
			names = {"--country", "-c"},
			description = " ISO 3166 alpha-2 country code",
			required = false,
			defaultValue = ""
		)
		String country;

		PurchasesOfForeigners(final BookStoreDemo bookStoreDemo)
		{
			super(bookStoreDemo);
		}

		@Override
		public void run()
		{
			final int year = this.year == 0
				? Year.now().getValue()
				: this.year;

			if(this.country.isEmpty())
			{
				final long count = this.data().purchases().countPurchasesOfForeigners(year);
				System.out.println("Purchases of foreigners in " + year);
				System.out.println(count);
			}
			else
			{
				final Country country = this.searchCountry(this.country);
				if(country == null)
				{
					System.out.println("Country not found");
				}
				else
				{
					final long count = this.data().purchases().countPurchasesOfForeigners(year, country);
					System.out.println("Purchases of foreigners in " + country.name() + " in " + year);
					System.out.println(count);
				}
			}
		}
	}

	@Command(
		name = "employeeOfTheYear",
		aliases = {"eoty"},
		description = "Prints the best performing employee of a specific country or worldwide.",
		mixinStandardHelpOptions = true
	)
	static class EmployeeOfTheYear extends Abstract
	{
		@Option(
			names = {"--year", "-y"},
			description = "the year for the calculation",
			required = false,
			defaultValue = "0"
		)
		int    year;

		@Option(
			names = {"--country", "-c"},
			description = " ISO 3166 alpha-2 country code",
			required = false,
			defaultValue = ""
		)
		String country;

		EmployeeOfTheYear(final BookStoreDemo bookStoreDemo)
		{
			super(bookStoreDemo);
		}

		@Override
		public void run()
		{
			final int year = this.year == 0
				? Year.now().getValue()
				: this.year;

			if(this.country.isEmpty())
			{
				final Employee employee = this.data().purchases().employeeOfTheYear(year);
				System.out.println("Employee of the year " + year);
				System.out.println(employee.name());
			}
			else
			{
				final Country country = this.searchCountry(this.country);
				if(country == null)
				{
					System.out.println("Country not found");
				}
				else
				{
					final Employee employee = this.data().purchases().employeeOfTheYear(year, country);
					System.out.println("Employee of the year " + year + " in " + country.name());
					System.out.println(employee.name());
				}
			}
		}
	}

	@Command(
		name = "exit",
		aliases = {"quit"},
		description = "Exits the program",
		mixinStandardHelpOptions = true
	)
	static class Exit extends Abstract
	{
		Exit(final BookStoreDemo bookStoreDemo)
		{
			super(bookStoreDemo);
		}

		@Override
		public void run()
		{
			this.bookStoreDemo.shutdown();

			System.out.println("Bye!");

			System.exit(0);
		}
	}

}
