package de.keycloak.credential;

import com.google.auto.service.AutoService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Metrics;
import org.keycloak.Config;
import org.keycloak.credential.CredentialProviderFactory;
import org.keycloak.credential.PasswordCredentialProvider;
import org.keycloak.credential.PasswordCredentialProviderFactory;
import org.keycloak.models.KeycloakSession;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * We extend the default {@link PasswordCredentialProviderFactory} which has unfortunately a lot of private fields and constants.
 * Thus, we have to copy the relevant code parts here. Be careful to check this on every Keycloak version update, at least major updates!
 * <p>
 * Inspired from <a href="https://github.com/giz-berlin/keycloak/pull/1">https://github.com/giz-berlin/keycloak/pull/1</a>
 */
@AutoService(CredentialProviderFactory.class)
public class CustomPasswordCredentialProviderFactory extends PasswordCredentialProviderFactory {

    private static final String HASHES_COUNTER_TAGS = "validations-counter-tags";
    private static final String KEYCLOAK_METER_NAME_PREFIX = "keycloak.";
    private static final String LOGIN_PASSWORD_VERIFY_METER_NAME = KEYCLOAK_METER_NAME_PREFIX + "credentials.password.hashing";
    private static final String LOGIN_PASSWORD_VERIFY_METER_DESCRIPTION = "Password validations";

    private static final String HASHES_COUNTER_TAGS_DEFAULT_VALUE = String.format("%s,%s,%s,%s", METER_REALM_TAG, METER_ALGORITHM_TAG, METER_HASHING_STRENGTH_TAG, METER_VALIDATION_OUTCOME_TAG);

    private boolean metricsEnabled;
    private boolean withRealmInMetric;
    private boolean withAlgorithmInMetric;
    private boolean withHashingStrengthInMetric;
    private boolean withOutcomeInMetric;

    private Meter.MeterProvider<Counter> meterProvider;

    @Override
    public PasswordCredentialProvider create(KeycloakSession session) {
        return new CustomPasswordCredentialProvider(session, meterProvider, metricsEnabled, withRealmInMetric, withAlgorithmInMetric, withHashingStrengthInMetric, withOutcomeInMetric);
    }

    @Override
    public void init(Config.Scope config) {
        metricsEnabled = config.getBoolean("metrics-enabled", false);
        if (metricsEnabled) {
            meterProvider = Counter.builder(LOGIN_PASSWORD_VERIFY_METER_NAME)
                .description(LOGIN_PASSWORD_VERIFY_METER_DESCRIPTION)
                .baseUnit("validations")
                .withRegistry(Metrics.globalRegistry);

            Set<String> tags = Arrays.stream(config.get(HASHES_COUNTER_TAGS, HASHES_COUNTER_TAGS_DEFAULT_VALUE).split(",")).collect(Collectors.toSet());
            withRealmInMetric = tags.contains(METER_REALM_TAG);
            withAlgorithmInMetric = tags.contains(METER_ALGORITHM_TAG);
            withHashingStrengthInMetric = tags.contains(METER_HASHING_STRENGTH_TAG);
            withOutcomeInMetric = tags.contains(METER_VALIDATION_OUTCOME_TAG);
        }
    }

    @Override
    public int order() {
        return super.order() + 10;
    }
}

