package be.nabu.libs.services.pojo.converters;

import be.nabu.libs.converter.api.ConverterProvider;
import be.nabu.libs.services.DefinedServiceResolverFactory;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.DefinedServiceResolver;

public class StringToDefinedService implements ConverterProvider<String, DefinedService> {

	private DefinedServiceResolver resolver;

	public StringToDefinedService() {
		this(DefinedServiceResolverFactory.getInstance().getResolver());
	}
	
	public StringToDefinedService(DefinedServiceResolver resolver) {
		this.resolver = resolver;
	}
	
	@Override
	public DefinedService convert(String instance) {
		return instance == null ? null : resolver.resolve(instance);
	}

	@Override
	public Class<String> getSourceClass() {
		return String.class;
	}

	@Override
	public Class<DefinedService> getTargetClass() {
		return DefinedService.class;
	}

}
