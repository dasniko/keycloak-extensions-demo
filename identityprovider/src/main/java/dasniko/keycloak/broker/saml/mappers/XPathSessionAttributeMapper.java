package dasniko.keycloak.broker.saml.mappers;

import com.google.auto.service.AutoService;
import de.keycloak.annotation.CopiedFromKeycloak;
import de.keycloak.provider.DefaultServerInfoAware;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityProviderMapper;
import org.keycloak.broker.saml.SAMLEndpoint;
import org.keycloak.broker.saml.mappers.XPathAttributeMapper;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.saml.common.util.DocumentUtil;
import org.w3c.dom.Document;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@JBossLog
@AutoService(IdentityProviderMapper.class)
public class XPathSessionAttributeMapper extends XPathAttributeMapper implements SessionAttributeMapper, DefaultServerInfoAware {

	public static final String PROVIDER_ID = "saml-xpath-session-attribute-idp-mapper";

	private static final Pattern NAMESPACE_PATTERN = Pattern.compile("xmlns:(\\w+)=\"(.+?)\"");
	private static final ThreadLocal<XPathFactory> XPATH_FACTORY = ThreadLocal.withInitial(() -> {
		final XPathFactory xPathFactory = XPathFactory.newInstance();
		xPathFactory.setXPathVariableResolver(variableName -> {
			throw new UnsupportedOperationException("resolveVariable for variable " + variableName + " not supported");
		});
		xPathFactory.setXPathFunctionResolver((functionName, arity) -> {
			throw new UnsupportedOperationException("resolveFunction for function " + functionName + " not supported");
		});
		return xPathFactory;
	});

	@Override
	public String getDisplayCategory() {
		return "SAML Attribute Mapper";
	}

	@Override
	public String getDisplayType() {
		return "XPath Session Attribute Importer";
	}

	@Override
	public String getHelpText() {
		return "Extract text of a saml attribute via XPath expression and import into the specified session attribute.";
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
		String attributeXPath = mapperModel.getConfig().get(ATTRIBUTE_XPATH);
		return findAttributeValuesInContext(attributeName, attributeXPath, context);
	}

	@CopiedFromKeycloak(version = "26.6.0", source = "org.keycloak.broker.saml.mappers.XPathAttributeMapper", reason = "private method")
	private static Function<String, Object> applyXPath(String attributeXPath) {
		return xml -> {
			try {
				log.tracef("Trying to parse: %s", xml);

				Matcher namespaceMatcher = NAMESPACE_PATTERN.matcher(xml);
				Map<String, String> namespaces = new HashMap<>();
				Map<String, String> prefixes = new HashMap<>();
				while (namespaceMatcher.find()) {
					namespaces.put(namespaceMatcher.group(1), namespaceMatcher.group(2));
					prefixes.put(namespaceMatcher.group(2), namespaceMatcher.group(1));
				}

				XPath xPath = XPATH_FACTORY.get().newXPath();
				xPath.setNamespaceContext(new NamespaceContext() {
					@Override
					public String getNamespaceURI(String prefix) {
						if (namespaces.containsKey(prefix)) {
							return namespaces.get(prefix);
						}

						return XMLConstants.NULL_NS_URI;
					}

					@Override
					public String getPrefix(String namespaceURI) {
						if (prefixes.containsKey(namespaceURI)) {
							return prefixes.get(namespaceURI);
						}

						return null;
					}

					@Override
					public Iterator<String> getPrefixes(String namespaceURI) {
						List<String> list = new ArrayList<>();
						if (prefixes.containsKey(namespaceURI)) {
							list.add(prefixes.get(namespaceURI));
						}

						return list.iterator();
					}
				});
				Document document = DocumentUtil.getDocument(new StringReader(xml));
				return xPath.compile(attributeXPath).evaluate(document, XPathConstants.STRING);
			} catch (XPathExpressionException | UnsupportedOperationException e) {
				log.warn("Unparsable element will be ignored", e);
				return "";
			} catch (Exception e) {
				throw new RuntimeException("Could not parse xml element", e);
			}
		};
	}

	@CopiedFromKeycloak(version = "26.6.0", source = "org.keycloak.broker.saml.mappers.XPathAttributeMapper", reason = "private method")
	private List<String> findAttributeValuesInContext(String attributeName, String attributeXPath, BrokeredIdentityContext context) {
		AssertionType assertion = (AssertionType) context.getContextData().get(SAMLEndpoint.SAML_ASSERTION);

		return assertion.getAttributeStatements().stream()
			.map(AttributeStatementType::getAttributes)
			.flatMap(Collection::stream)
			.filter(elementWith(attributeName))
			.map(AttributeStatementType.ASTChoiceType::getAttribute)
			.map(AttributeType::getAttributeValue)
			.flatMap(Collection::stream)
			.filter(String.class::isInstance)
			.map(Object::toString)
			.map(s -> "<root>" + s + "</root>")
			.map(applyXPath(attributeXPath))
			.filter(Objects::nonNull)
			.map(Object::toString)
			.filter(x -> !x.isEmpty())
			.collect(Collectors.toList());
	}

	@CopiedFromKeycloak(version = "26.6.0", source = "org.keycloak.broker.saml.mappers.XPathAttributeMapper", reason = "private method")
	private Predicate<AttributeStatementType.ASTChoiceType> elementWith(String attributeName) {
		return attributeType -> {
			AttributeType attribute = attributeType.getAttribute();
			return attributeName == null
				|| Objects.equals(attribute.getName(), attributeName)
				|| Objects.equals(attribute.getFriendlyName(), attributeName);
		};
	}
}
