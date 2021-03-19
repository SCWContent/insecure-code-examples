# SQL Injection

## What is SQL Injection

SQL Injection occurs when a user passes in to the application some data which is used directly in an SQL statement. This might allow them to:

- View data the user does not have permission to see.
- Manipulate the database and create, amend or delete data in the database.
- Change the structure of the database by dropping tables.
- Blind injection to 'guess' data or the structure of the database.

SQL Injection is such a common attack vector for Hackers that tools such as [sqlmap](https://github.com/sqlmapproject/sqlmap) exist to automate the process of detecting and exploiting the vulnerability.

## Real World

In a web application an SQL Injection vulnerability might be exploited by a form on the front end where the user types in a partial SQL query instead of the expected value.

If the application lacks validation, overly trusts the input data, and builds queries directly from input strings then an input such as `'` might be enough to trigger a visible error on screen that alerts the user to the possibility of an SQL Injection vulnerability.

Once the 'possibility' for an SQL injection has been established, experimentation with the input might result in viewing more data than expected e.g. if the user entered `' or 1==1; -- ` and this value was injected into a query such as:

```sql
"SELECT * from users where name='%s'"
```

Then the user might see more information about users than they were supposed to.

The following code is vulnerable to SQL Injection.

~~~~~~~~
    public List<String> getTodosOfStatus(String status) {

        List<String> todos = new ArrayList<>();

        try {
            Statement stm = dbConnection.createStatement();
            ResultSet res = stm.executeQuery("SELECT description from todos where status=" + status);

            while(res.next()){
                todos.add(res.getString("description"));
            }

            stm.close();

        }catch(Exception e){
            logger.log(Level.SEVERE, e.getMessage());
        }

        return todos;
    }
~~~~~~~~

Assuming that there are no additional input validations around the `status` `String` and that the `status` is passed in externally from the user, the above code is vulnerable to SQL Injection. e.g. the status could be amended from "1" to an SQL statement that retrieves  user names and passwords from the database:

~~~~~~~~
"-1 UNION SELECT name || '~' || password as description from users"
~~~~~~~~

## Technical Implementation Details

The SQL Injection examples in this project are all documented as tests.

This has the benefit that:

- the database used is an in memory database, it is fast and will be installed via maven
- the examples are easy to experiment with by amending the code.
    - Try amending the SQL injection statements and see what results you get.
- no need to run docker to isolate the insecure database
- illustrates using `@Test` to create integration tests to check for SQL Injection

The tests are a useful way to expand your SQL Injection skills without having to work with an SQL environment and it was easy to amend the `MyDB` file to create new tables and relationships to experiment with SQL Injection, rather than amend and deploy an entire application.

## Project Overview

This section provides a short overview of the code, and links to more details in readme's contained in the project.

## DbApi (main/java/sqlinjection/insecuredb)

The [DbApi](../../../main/java/sqlinjection/insecuredb) class is vulnerable to SQL Injection it passes `String` into an `executeQuery` method.

The [SecureDbApi](../../../main/java/sqlinjection/secureddb) class is not vulnerable. The main difference is the use of `PreparedStatement`.

The `basic-protection-set` Sensei cookbook can make the code changes required to convert the code in `DbApi` to use `PreparedStatement` but it worth examining the code to make sure you understand the differences.

## SQL Injection Variation

The fun part of this project is in the `test` code.

The tests contain examples of variations in SQL Injection formats to exploit the weaknesses in the DbApi.

As a learning exercise, look through these and make sure you understand them and see if you can add any more examples of SQL Injection payloads that will exploit the weaknesses in the DbApi.

When you fix the issues in DbApi then the sqlinjection tests will fail.

Sensei was originally implemented as a a tool to find insecure code and fix it to become secure code.  Given that secure code is generally perceived as higher quality code, the mental model around Sensei became a tool for identifying low quality code and fixing it to become high quality code.

This means that Sensei does more than fix security defects, but it also means that Sensei has dedicated functionality aimed at helping identify security defects which a more general code quality tool might not have.

In this example we will explore some of those features by looking at an SQL Injection use case.

## Sensei Detection

Security and SQL Injection is one of the original use cases that Sensei was designed to handle.

To find the issue of using `executeQuery` without a `PreparedStatement` I would search for:

- a method call of `executeQuery`
- on a `java.sql.Statement`
- which contains untrusted input

The 'contains untrusted input' concept, was added to Sensei to help identify injection issues.

Sensei views an 'untrusted input' as any String that cannot be determined at compile time e.g. mutable fields, parameters, and non-constant variables.

When I use `alt+enter` on the `executeQuery` to create a new recipe, I will be offered more options.

- `parameterize this call`
- `search for similar methodcalls`
- `start from scratch`


In this case I want to `parameterize this call`, and I'll name and describe the recipe.

```
name: execute query with untrusted inputs is vulnerable to SQL Injection
description: execute query with untrusted inputs is vulnerable to SQL Injection
```

## Parameterize this call

`parameterize this call` will automatically create a search for any `executeQuery` method on a `java.sql.Statement` with an argument which `containsUntrustedInput`.

The only change I need to make is to set the type of the input to `java.lang.String`.

The Yaml description of this search would be:

~~~~~~~~
search:
  methodcall:
    args:
      1:
        containsUntrustedInput: true
        type: "java.lang.String"
    name: "executeQuery"
    type: "java.sql.Statement"
~~~~~~~~

This would be the type of rule we would expect to find in a SAST (Static Application Security Testing) tool, to highlight and explain the issue.

## QuickFix

The quick fix that is automatically generated performs some of the work for us:

~~~~~~~~
availableFixes:
- actions:
  - parameterize:
      placeholderFormat: "?"
      extractUntrustedInput:
        methodsOnObject: {}
~~~~~~~~

which removes the `untrusted input` and adds the parameterized string replacement placeholder:

```
stm = dbConnection.createStatement();
            return stm.executeQuery("SELECT description from todos where status=?");
```

I will give this a `name` so that I can use it from the `alt+enter`.

```
name: convert to a preparedStatement
```


I want to change the type of the calling object to a `java.sql.PreparedStatement`

```
availableFixes:
- actions:
  - parameterize:
      placeholderFormat: "?"
      extractUntrustedInput:
        methodsOnObject: {}
  - changeTypeOfCallObject:
      type: "java.sql.PreparedStatement"        
```


### Amending from `createStatement` to `prepareStatement`

Having changed the variable type to a `PreparedStatement` I need to amend `createStatement` to a `prepareStatement`.

I don't just want to extract the Untrusted Input, I want to change the assignment code.

To do that I can use `rewriteLastAssignment` to change the assignment where I declared and assigned the variable.

```
{{{ qualifier }}}.prepareStatement({{{ markedElement.arguments.0}}}{{#arguments}}, {{{.}}}{{/arguments}})
```


### Amending `executeQuery`

With the `preparedStatement` declaration now containing the templated query.

I really need to modify the arguments from the `executeQuery` method call, to remove the argument.

```
  - modifyArguments:
      remove:
      - 1
```

### Setting the Parameters in the prepared statement

Next I really need to setup the parameters in the prepared statement.

I'll do this by expanding the configuration of the `extractUntrustedInput`

Currently this does nothing with the extracted information.

so I need to define a method to create when extracting the untrusted input.

I add a method using `methods`, with a `methodName` of `setString` because I matched on a `String` parameter and I know that my argument is a String.

I need to tell Sensei where to create the methods. So I
add the target to the `methods` transformation.

The target being the `subject`, which is the `executeQuery` method call we matched and we want the new method call to be before this statement.

```
          target:
            subject:
              insertBefore: true
```

And now that the method is shown in the preview I can setup the arguments `args` as:

- 1: being the index in the template. I could hard code this as "1", but I'll use the `index` of the extracted untrusted input `{{{ index }}}`.
- 2: being the value to add, which is the untrusted variable `{{{ . }}}


```
        methodsOnObject:
          methods:
          - type: "java.lang.String"
            methodName: "setString"
            args:
              "1": "{{{ index }}}"
              "2": "{{{ . }}}"
```


## Complete

The YAML full QuickFix I created for this simple example is shown below:

```
availableFixes:
- name: "convert to a prepared statement"
  actions:
  - parameterize:
      placeholderFormat: "?"
      extractUntrustedInput:
        methodsOnObject:
          methods:
          - methodName: "setString"
            args:
              "1": "{{{ index }}}"
              "2": "{{{ . }}}"
          target:
            subject:
              insertBefore: true
  - changeTypeOfCallObject:
      rewriteLastAssignment: "{{{ qualifier }}}.prepareStatement({{{ markedElement.arguments.0}}}{{#arguments}},\
        \ {{{.}}}{{/arguments}})"
      type: "java.sql.PreparedStatement"
  - modifyArguments:
      remove:
      - 1
```

And this converts the insecure code:

```
Statement stm = dbConnection.createStatement();
ResultSet res = stm.executeQuery("SELECT description from todos where status=" + status);
```

To:

```
PreparedStatement stm = dbConnection.prepareStatement("SELECT description from todos where status=?");
stm.setString(1, status);
ResultSet res = stm.executeQuery();
```

The recipes in the basic-protection-set are more advanced than this recipe and cover more variation in SQL Injection.

- [github.com/SecureCodeWarrior/cookbook-basic-protection-set](https://github.com/SecureCodeWarrior/cookbook-basic-protection-set)

