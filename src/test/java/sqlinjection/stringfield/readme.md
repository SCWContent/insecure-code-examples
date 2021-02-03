Sensei was originally implemented as a a tool to find insecure code and fix it to become secure code.  Given that secure code is generally perceived as higher quality code, the mental model around Sensei became a tool for identifying low quality code and fixing it to become high quality code.

This means that Sensei does more than fix security defects, but it also means that Sensei has dedicated functionality aimed at helping identify security defects which a more general code quality tool might not have.

In this example we will explore some of those features by looking at an SQL Injection use case.

## SQL Injection

SQL Injection is a security vulnerability that a user, or attacker, can use to manipulate the database queries and inject their own SQL code into the running application. This can be used to change data in the database, potentially damage the structure of the database, and to expose information that should not be accessible to the end user.

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

Assuming that there are no additional input validations around the `status` `String` and that the `status` is passed in externally from the user, the above code is vulnerable to SQL Injection there the status could be amended from "1" to an SQL statement that retrieves  user names and passwords from the database:

~~~~~~~~
"-1 UNION SELECT name || '~' || password as description from users"
~~~~~~~~

## Sensei Detection

This is one of the original use cases that Sensei was designed to handle.

To find this issue I would search for:

- a method call of `executeQuery`
- on  a `java.sql.Statement` 
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

_Authors Note: The above wasn't as easy as I'd hoped because there is currently no mustache helper in the gui for these variables, and this isn't listed in the documentation. I've raised this as bug so we'll get it fixed in a future release._

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

_Authors note: again, this wasn't as easy because there is no mustache template help for this in the GUI. We'll fix this in a future release._

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



