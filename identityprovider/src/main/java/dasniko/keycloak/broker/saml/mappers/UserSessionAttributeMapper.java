package dasniko.keycloak.broker.saml.mappers;

import com.google.auto.service.AutoService;
import de.keycloak.annotation.CopiedFromKeycloak;
import de.keycloak.provider.DefaultServerInfoAware;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityProviderMapper;
import org.keycloak.broker.saml.SAMLEndpoint;
import org.keycloak.broker.saml.mappers.UserAttributeMapper;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@JBossLog
@AutoService(IdentityProviderMapper.class)
public class UserSessionAttributeMapper extends UserAttributeMapper implements SessionAttributeMapper, DefaultServerInfoAware {

	public static final String PROVIDER_ID = "saml-user-session-attribute-idp-mapper";

	@Override
	public String getDisplayCategory() {
		return "SAML Attribute Mapper";
	}

	@Override
	public String getDisplayType() {
		return "Session Attribute Importer";
	}

	@Override
	public String getHelpText() {
		return "Import declared saml attribute if it exists in assertion into the specified session attribute.";
	}

	@Override
	public List<ProviderConfigProperty> getConfigProperties() {
		return extendConfigProperties(super.getConfigProperties());
	}

	@Override
	public String getId() {
		return PROVIDER_ID;
	}

	@Override
	public void preprocessFederatedIdentity(KeycloakSession session, RealmModel realm, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
		setSessionAttribute(mapperModel, context);
	}

	@Override
	public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
		setSessionAttribute(mapperModel, context);
	}

	@Override
	public void importNewUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
		setSessionAttribute(mapperModel, context);
	}

	@Override
	public List<String> findAttributesInContext(IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context, String attributeName) {
		return findAttributeValuesInContext(attributeName, context);
	}

	@CopiedFromKeycloak(version = "26.6.0", source = "org.keycloak.broker.saml.mappers.UserAttributeMapper", reason = "private method")
	private List<String> findAttributeValuesInContext(String attributeName, BrokeredIdentityContext context) {
		AssertionType assertion = (AssertionType) context.getContextData().get(SAMLEndpoint.SAML_ASSERTION);

		return assertion.getAttributeStatements().stream()
			.flatMap(statement -> statement.getAttributes().stream())
			.filter(elementWith(attributeName))
			.flatMap(attributeType -> attributeType.getAttribute().getAttributeValue().stream())
			.filter(Objects::nonNull)
			.map(Object::toString)
			.collect(Collectors.toList());
	}

	@CopiedFromKeycloak(version = "26.6.0", source = "org.keycloak.broker.saml.mappers.UserAttributeMapper", reason = "private method")
	private Predicate<AttributeStatementType.ASTChoiceType> elementWith(String attributeName) {
		return attributeType -> {
			AttributeType attribute = attributeType.getAttribute();
			return attributeName == null
				|| Objects.equals(attribute.getName(), attributeName)
				|| Objects.equals(attribute.getFriendlyName(), attributeName);
		};
	}

}
