# SQL Injection

## What is SQL Injection

SQL Injection is when a user passes in to the application some data which is used directly in an SQL statement. This might be to:

- view data the user does not have permission to see.
- manipulate the database and create, amend or delete data in the database.
- change the structure of the database by dropping tables
- blind injection to 'guess' data or the structure of the database

SQL Injection is such a common attack vector for Hackers that tools such as [sqlmap](http://sqlmap.org/) exist to automate the process of detecting and exploiting the vulnerability.

## Real World

In a web application an SQL Injection vulnerability might be exploited by a form on the front end where the user types in a partial SQL query instead of the expected value.

If the application lacks validation, overly trusts the input data, and builds queries directly from input strings then an input such as `'` might be enough to trigger a visible error on screen that alerts the user to the possibility of an SQL Injection vulnerability.

Once the 'possibility' for an SQL injection has been established, experimentation with the input might result in viewing more data than expected e.g. if the user entered `' or 1==1; -- ` and this value was injected into a query such as:

```sql
"SELECT * from users where name='%s'"
```

Then the user might see more information about users than they were supposed to.

## Technical Implementation Details

The SQL Injection examples are all documented as tests.

This has the benefit that:

- the database used is an in memory database, it is fast and will be installed via maven
- the examples are easy to experiment with by amending the code.
    - Try amending the SQL injection statements and see what results you get.
- no need to run docker to isolate the insecure database
- illustrates using `@Test` to create integration tests to check for SQL Injection

I found these tests a useful way to expand my SQL Injection skills without having to work with an SQL environment and it was easy to amend the `MyDB` file to create new tables and relationships to experiment with SQL Injection, rather than amend and deploy and entire application.

