/*******************************************************************************
 * Copyright (c) 2000-2011 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 *******************************************************************************/

package com.liferay.ide.eclipse.portlet.ui.wizard;

import com.liferay.ide.eclipse.core.util.CoreUtil;
import com.liferay.ide.eclipse.portlet.ui.PortletUIPlugin;
import com.liferay.ide.eclipse.project.core.util.ProjectUtil;
import com.liferay.ide.eclipse.server.core.ILiferayRuntime;
import com.liferay.ide.eclipse.server.util.ServerUtil;
import com.liferay.ide.eclipse.ui.dialog.FilteredTypesSelectionDialogEx;
import com.liferay.ide.eclipse.ui.wizard.StringArrayTableWizardSection;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.internal.core.search.BasicSearchEngine;
import org.eclipse.jdt.internal.ui.dialogs.FilteredTypesSelectionDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jst.j2ee.internal.common.operations.INewJavaClassDataModelProperties;
import org.eclipse.jst.j2ee.internal.plugin.J2EEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;

/**
 * @author Greg Amerson
 */
@SuppressWarnings("restriction")
public class ServicesTableWizardSection extends StringArrayTableWizardSection {

	public class AddServiceDialog extends AddStringArrayDialog {

		protected String[] buttonLabels;

		public AddServiceDialog(Shell shell, String windowTitle, String[] labelsForTextField, String[] buttonLabels) {
			super(shell, windowTitle, labelsForTextField);

			setShellStyle(getShellStyle() | SWT.RESIZE);

			this.buttonLabels = buttonLabels;

			setWidthHint(450);
		}

		@Override
		protected Text createField(Composite parent, final int index) {
			Label label = new Label(parent, SWT.LEFT);
			label.setText(labelsForTextField[index]);
			label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

			// Composite composite = new Composite(parent, SWT.NONE);
			// GridData data = new GridData(GridData.FILL_HORIZONTAL);
			// composite.setLayoutData(data);
			// composite.setLayout(new GridLayout(2, false));
			final Text text = new Text(parent, SWT.SINGLE | SWT.BORDER);

			GridData data = new GridData(GridData.FILL_HORIZONTAL);
			// data.widthHint = 200;

			text.setLayoutData(data);

			Composite buttonComposite = new Composite(parent, SWT.NONE);

			String[] buttonLbls = buttonLabels[index].split(",");

			GridLayout gl = new GridLayout(buttonLbls.length, true);
			gl.marginWidth = 0;
			gl.horizontalSpacing = 1;

			buttonComposite.setLayout(gl);

			for (final String lbl : buttonLbls) {
				Button button = new Button(buttonComposite, SWT.PUSH);
				button.setText(lbl);
				button.addSelectionListener(new SelectionAdapter() {

					@Override
					public void widgetSelected(SelectionEvent e) {
						handleArrayDialogButtonSelected(index, lbl, text);
					}
				});
			}

			return text;
		}

		protected void handleArrayDialogButtonSelected(int index, String label, Text text) {
			if (index == 0) { // select event
				handleSelectServiceButton(text);
			}
			else if (index == 1 && "Select...".equals(label)) {
				handleSelectImplClassButton(text);
			}
			else if (index == 1 && "New...".equals(label)) {
				handleNewImplClassButton(text);
			}
		}

		protected void handleNewImplClassButton(Text text) {
			if (CoreUtil.isNullOrEmpty(texts[0].getText())) {
				MessageDialog.openWarning(getParentShell(), "Add Service", "Please specify a service type first.");

				return;
			}

			String serviceType = texts[0].getText();

			String wrapperType = "";

			if (serviceType.endsWith("Service")) {
				wrapperType = serviceType + "Wrapper";
			}

			NewEventActionClassDialog dialog =
				new NewServiceWrapperClassDialog(getShell(), model, serviceType, wrapperType);

			if (dialog.open() == Window.OK) {
				String qualifiedClassname = dialog.getQualifiedClassname();

				text.setText(qualifiedClassname);
			}
		}

		protected void handleSelectImplClassButton(Text text) {
			if (CoreUtil.isNullOrEmpty(texts[0].getText())) {
				MessageDialog.openWarning(getParentShell(), "Add Service", "Please specify a service type first.");

				return;
			}

			IPackageFragmentRoot packRoot =
				(IPackageFragmentRoot) model.getProperty(INewJavaClassDataModelProperties.JAVA_PACKAGE_FRAGMENT_ROOT);

			if (packRoot == null) {
				return;
			}

			IJavaSearchScope scope = null;

			try {
				// get the Service type and replace Service with Wrapper and
				// make it the supertype
				String serviceType = texts[0].getText();

				if (serviceType.endsWith("Service")) {
					String wrapperType = serviceType + "Wrapper";

					scope = BasicSearchEngine.createHierarchyScope(packRoot.getJavaProject().findType(wrapperType));
				}
			}
			catch (JavaModelException e) {
				PortletUIPlugin.logError(e);

				return;
			}

			FilteredTypesSelectionDialog dialog =
				new FilteredTypesSelectionDialogEx(getShell(), false, null, scope, IJavaSearchConstants.CLASS);
			dialog.setTitle(J2EEUIMessages.SUPERCLASS_SELECTION_DIALOG_TITLE);
			dialog.setMessage(J2EEUIMessages.SUPERCLASS_SELECTION_DIALOG_DESC);

			if (dialog.open() == Window.OK) {
				IType type = (IType) dialog.getFirstResult();

				String classFullPath = J2EEUIMessages.EMPTY_STRING;

				if (type != null) {
					classFullPath = type.getFullyQualifiedName();
				}

				text.setText(classFullPath);
			}
		}

		protected void handleSelectServiceButton(Text text) {
			PortalServiceSearchScope scope = new PortalServiceSearchScope();
			scope.setResourcePattern(new String[] {
				".*Service.class$"
			});

			IProject project = ProjectUtil.getProject(model);

			try {
				ILiferayRuntime runtime = ServerUtil.getLiferayRuntime(project);

				IPath[] libs = runtime.getAllUserClasspathLibraries();

				for (IPath lib : libs) {
					if (lib.lastSegment().equals("portal-service.jar")) {
						scope.setEnclosingJarPaths(new IPath[] {
							lib
						});

						break;
					}
				}
			}
			catch (CoreException e1) {
				PortletUIPlugin.logError(e1);
				return;
			}

			FilteredTypesSelectionDialog dialog =
				new FilteredTypesSelectionDialogEx(getShell(), false, null, scope, IJavaSearchConstants.INTERFACE);
			dialog.setTitle(J2EEUIMessages.SUPERCLASS_SELECTION_DIALOG_TITLE);
			dialog.setMessage(J2EEUIMessages.SUPERCLASS_SELECTION_DIALOG_DESC);

			if (dialog.open() == Window.OK) {
				IType type = (IType) dialog.getFirstResult();

				String classFullPath = J2EEUIMessages.EMPTY_STRING;

				if (type != null) {
					classFullPath = type.getFullyQualifiedName();
				}

				text.setText(classFullPath);
			}
		}

	}

	protected String[] buttonLabels;

	protected IProject project;

	protected File servicesPropertiesFile;

	public ServicesTableWizardSection(
		Composite parent, String componentLabel, String dialogTitle, String addButtonLabel, String editButtonLabel,
		String removeButtonLabel, String[] columnTitles, String[] fieldLabels, Image labelProviderImage,
		IDataModel model, String propertyName) {

		super(parent, componentLabel, dialogTitle, addButtonLabel, editButtonLabel, removeButtonLabel, columnTitles, fieldLabels, labelProviderImage, model, propertyName);

		this.buttonLabels = new String[] {
			"Select...", "Select...,New..."
		};

		this.servicesPropertiesFile = null;
	}

	public void setProject(IProject project) {
		this.project = project;
	}

	@Override
	protected void handleAddButtonSelected() {
		AddServiceDialog dialog = new AddServiceDialog(getShell(), dialogTitle, fieldLabels, buttonLabels);

		if (dialog.open() == Window.OK) {
			String[] stringArray = dialog.getStringArray();

			addStringArray(stringArray);
		}
	}

}
