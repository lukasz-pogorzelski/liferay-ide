package com.liferay.ide.eclipse.portlet.core;

import com.liferay.ide.eclipse.portlet.core.operation.NewPortletClassDataModelProvider;
import com.liferay.ide.eclipse.project.core.AbstractProjectDefinition;
import com.liferay.ide.eclipse.project.core.facet.IPluginFacetConstants;
import com.liferay.ide.eclipse.project.core.facet.IPluginProjectDataModelProperties;
import com.liferay.ide.eclipse.project.core.util.ProjectUtil;

import org.eclipse.jst.common.project.facet.IJavaFacetInstallDataModelProperties;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.project.facet.IJ2EEFacetConstants;
import org.eclipse.jst.j2ee.web.project.facet.IWebFacetInstallDataModelProperties;
import org.eclipse.wst.common.frameworks.datamodel.DataModelFactory;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.project.facet.core.IFacetedProjectWorkingCopy;

public class LiferayPortletDefinition extends AbstractProjectDefinition implements IPluginProjectDataModelProperties {

	protected IDataModel nestedModel;

	public LiferayPortletDefinition() {
		super();
	}

	public void setupNewProject(IDataModel dataModel, IFacetedProjectWorkingCopy facetedProject) {
		ProjectUtil.setGenerateDD(dataModel, true);

		FacetDataModelMap map = (FacetDataModelMap) dataModel.getProperty(FACET_DM_MAP);
		IDataModel webFacetModel = map.getFacetDataModel(IJ2EEFacetConstants.DYNAMIC_WEB_FACET.getId());

		webFacetModel.setStringProperty(
			IWebFacetInstallDataModelProperties.CONFIG_FOLDER, IPluginFacetConstants.PORTLET_PLUGIN_SDK_CONFIG_FOLDER);
		webFacetModel.setStringProperty(
			IWebFacetInstallDataModelProperties.SOURCE_FOLDER, IPluginFacetConstants.PORTLET_PLUGIN_SDK_SOURCE_FOLDER);
		IDataModel javaFacetModel = map.getFacetDataModel(JavaFacet.FACET.getId());
		javaFacetModel.setStringProperty(
			IJavaFacetInstallDataModelProperties.SOURCE_FOLDER_NAME,
			IPluginFacetConstants.PORTLET_PLUGIN_SDK_SOURCE_FOLDER);
		javaFacetModel.setStringProperty(
			IJavaFacetInstallDataModelProperties.DEFAULT_OUTPUT_FOLDER_NAME,
			IPluginFacetConstants.PORTLET_PLUGIN_SDK_DEFAULT_OUTPUT_FOLDER);

		if (dataModel.isNestedModel(PLUGIN_FRAGMENT_DM)) {
			dataModel.removeNestedModel(PLUGIN_FRAGMENT_DM);
		}

		if (nestedModel == null) {
			nestedModel = DataModelFactory.createDataModel(new NewPortletClassDataModelProvider());
		}

		dataModel.addNestedModel(PLUGIN_FRAGMENT_DM, nestedModel);
		dataModel.setStringProperty(PLUGIN_FRAGMENT_BUTTON_LABEL, "Create custom portlet class");
	}
}