package dasniko.keycloak.broker.saml.mappers;

import de.keycloak.annotation.CopiedFromKeycloak;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.services.ServicesLogger;
import org.keycloak.utils.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.keycloak.broker.oidc.mappers.UserAttributeMapper.USER_ATTRIBUTE;
import static org.keycloak.broker.saml.mappers.UserAttributeMapper.ATTRIBUTE_FRIENDLY_NAME;
import static org.keycloak.broker.saml.mappers.UserAttributeMapper.ATTRIBUTE_NAME;

public interface SessionAttributeMapper {

	String SESSION_ATTRIBUTE = "session.attribute";
	String SESSION_ATTRIBUTE_PREFIX = "dasniko.saml_prop_";
	String MAPPING_VALUES = "mapping.values";
	String MAPPING_VALUES_REGEX = "mapping.values.regex";

	default List<ProviderConfigProperty> extendConfigProperties(List<ProviderConfigProperty> existingConfigProperties) {
		List<ProviderConfigProperty> props = replaceUserAttributeWithSessionAttributeProperty(existingConfigProperties);
		addTransformationMappings(props);
		return props;
	}

	private List<ProviderConfigProperty> replaceUserAttributeWithSessionAttributeProperty(List<ProviderConfigProperty> existingConfigProperties) {
		ProviderConfigProperty sessionAttributeProperty = new ProviderConfigProperty();
		sessionAttributeProperty.setName(SESSION_ATTRIBUTE);
		sessionAttributeProperty.setLabel("Session Attribute Name");
		sessionAttributeProperty.setHelpText("Session attribute name to store saml attribute.");
		sessionAttributeProperty.setType(ProviderConfigProperty.STRING_TYPE);

		List<ProviderConfigProperty> configProperties = new ArrayList<>(existingConfigProperties);
		int i = 0;
		for (ProviderConfigProperty property : configProperties) {
			if (property.getName().equals(USER_ATTRIBUTE)) {
				break;
			}
			i++;
		}
		configProperties.remove(i);
		configProperties.add(i, sessionAttributeProperty);
		return configProperties;
	}

	private void addTransformationMappings(List<ProviderConfigProperty> configProperties) {
		List<ProviderConfigProperty> mappingConfigs = ProviderConfigurationBuilder.create()
			.property()
			.name(MAPPING_VALUES)
			.label("Value Mappings")
			.helpText("List of (case-sensitive) value mappings with original value (key) and replacement value (value).")
			.type(ProviderConfigProperty.MAP_TYPE)
			.add()
			.property()
			.name(MAPPING_VALUES_REGEX)
			.label("Regex Values")
			.helpText("If enabled values are interpreted as regular expressions.")
			.type(ProviderConfigProperty.BOOLEAN_TYPE)
			.add()
			.build();

		configProperties.addAll(mappingConfigs);
	}

	default void setSessionAttribute(IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
		String attribute = mapperModel.getConfig().get(SESSION_ATTRIBUTE);
		if (StringUtil.isNullOrEmpty(attribute)) {
			return;
		}

		String attributeName = getAttributeNameFromMapperModel(mapperModel);

		List<String> attributeValuesInContext = findAttributesInContext(mapperModel, context, attributeName);
		if (attributeValuesInContext != null && !attributeValuesInContext.isEmpty()) {
			if (attributeValuesInContext.size() > 1) {
				ServicesLogger.LOGGER.warnf("Attribute '%s' has more than one value. Discarding all but the first.", attributeName);
			}

			String attributeValue = transformAttributeValue(mapperModel, attributeValuesInContext.getFirst());
			context.setSessionNote(SESSION_ATTRIBUTE_PREFIX + attribute, attributeValue);
		}
	}

	List<String> findAttributesInContext(IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context, String attributeName);

	default String transformAttributeValue(IdentityProviderMapperModel mapperModel, String attrValue) {
		if (StringUtil.isNullOrEmpty(attrValue)) {
			return attrValue;
		}

		AtomicReference<String> value = new AtomicReference<>(attrValue);
		boolean isRegex = Boolean.parseBoolean(mapperModel.getConfig().getOrDefault(MAPPING_VALUES_REGEX, "false"));
		mapperModel.getConfigMap(MAPPING_VALUES).forEach((key, val) -> {
			String v = val.getFirst();
			if (isRegex) {
				if (attrValue.matches(key)) {
					value.set(attrValue.replaceAll(key, v));
				}
			} else {
				if (attrValue.contains(key)) {
					value.set(attrValue.replace(key, v));
				}
			}
		});

		return value.get();
	}

	@CopiedFromKeycloak(version = "26.6.0", source = "org.keycloak.broker.saml.mappers.UserAttributeMapper", reason = "private method")
	private String getAttributeNameFromMapperModel(IdentityProviderMapperModel mapperModel) {
		String attributeName = mapperModel.getConfig().get(ATTRIBUTE_NAME);
		if (attributeName == null) {
			attributeName = mapperModel.getConfig().get(ATTRIBUTE_FRIENDLY_NAME);
		}
		return attributeName;
	}

}
