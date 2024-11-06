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
