Public Transport Enabler
========================

This is a Java library allowing you to get data from public transport providers.
Look into [NetworkProvider.java](https://github.com/schildbach/public-transport-enabler/blob/master/src/de/schildbach/pte/NetworkProvider.java) for an overview of the API.

Using providers that require secrets
------------------------------------

For some providers a secret like an API key is required to use their API.
Copy the `secrets.properties.template` file to `secrets.properties` like so:

    $ cp test/de/schildbach/pte/live/secrets.properties.template test/de/schildbach/pte/live/secrets.properties

You need to request the secrets directly from the provider. For Navitia based providers, you can [request a secret here](https://www.navitia.io/register).

How to run live tests?
----------------------

Make sure the test you want to run does not require a secret and if it does, see above for how to get one.
Once you have the secret or if your provider does not need one, you can run the tests in your IDE.
Both IntelliJ and Eclipse have excellent support for JUnit tests.

If you prefer to run tests from the command line, you can comment out the test exclude at the end of
[build.gradle](https://github.com/schildbach/public-transport-enabler/blob/master/build.gradle#L30)
and use this command to only execute a test for a single provider:

    $ gradle -Dtest.single=ParisProviderLive test

This uses the `ParisProvider` as an example.
Just replace it with the provider you want to test.
