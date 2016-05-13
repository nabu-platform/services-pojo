package be.nabu.libs.services.pojo.converters;

import be.nabu.libs.converter.api.ConverterProvider;
import be.nabu.libs.services.DefinedServiceInterfaceResolverFactory;
import be.nabu.libs.services.api.DefinedServiceInterface;
import be.nabu.libs.services.api.DefinedServiceInterfaceResolver;

public class StringToDefinedServiceInterface implements ConverterProvider<String, DefinedServiceInterface> {

	private DefinedServiceInterfaceResolver resolver;

	public StringToDefinedServiceInterface() {
		this(DefinedServiceInterfaceResolverFactory.getInstance().getResolver());
	}
	
	public StringToDefinedServiceInterface(DefinedServiceInterfaceResolver resolver) {
		this.resolver = resolver;
	}
	
	@Override
	public DefinedServiceInterface convert(String instance) {
		return instance == null ? null : resolver.resolve(instance);
	}

	@Override
	public Class<String> getSourceClass() {
		return String.class;
	}

	@Override
	public Class<DefinedServiceInterface> getTargetClass() {
		return DefinedServiceInterface.class;
	}

}
