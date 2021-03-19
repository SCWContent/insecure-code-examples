# Insecure Code Examples

A set of insecure code examples for demo purposes.

## Injection

Injection is still the number one Security Risk identified by OWASP in their "Top Ten" project.

- https://owasp.org/www-project-top-ten

The [SQL Injection](src/test/java/sqlinjection) package contains examples for:

- How SQL Injection can be exploited.
- Sample code vulnerable to SQL Injection.
- Sample secured code.

[Learn more about SQL Injection](src/test/java/sqlinjection)

## Sensei

If you have the [Secure Code Warrior Sensei plugin](https://securecodewarrior.com/sensei) installed then default "Remotely configured cookbooks" `basic-protection-set` cookbook has recipes that will detect the code vulnerable to SQL Injection in this project.

In the "Sensei Cookbooks" panel, click on the `basic-protection-set` and click the `[Inspect code with cookbook]` button to run all the `basic-protection-set` recipes against the code.

Fixing the code is easier than all the work the hacker had to go through to find SQL queries which could exploit the vulnerabilities.

## DbApi

In `main\java\sqlinjection\tododb` the DbApi class is vulnerable to SQL Injection.

The `SecureDbApi` class is not vulnerable. The main difference is the use of `PreparedStatement`.

The `basic-protection-set` Sensei cookbook can make the code changes required to convert the code to use `PreparedStatement` but it worth examining the code to make sure you understand the differences.

## SQL Injection Variation

The fun part of this project is in the `test` code.

The tests contain examples of variations in SQL Injection formats to exploit the weaknesses in the DbApi.

As a learning exercise, look through these and make sure you understand them and see if you can add any more examples of SQL Injection payloads that will exploit the weaknesses in the DbApi.

---

Copyright 2021 [Secure Code Warrior](https://securecodewarrior.com)




