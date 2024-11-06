/*
* Copyright (C) 2016 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

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
