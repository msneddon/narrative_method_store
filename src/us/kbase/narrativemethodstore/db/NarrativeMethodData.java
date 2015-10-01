package us.kbase.narrativemethodstore.db;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.fasterxml.jackson.databind.JsonNode;

import us.kbase.common.service.UObject;
import us.kbase.narrativemethodstore.AutoGeneratedValue;
import us.kbase.narrativemethodstore.CheckboxOptions;
import us.kbase.narrativemethodstore.DropdownOption;
import us.kbase.narrativemethodstore.DropdownOptions;
import us.kbase.narrativemethodstore.FixedMethodParameter;
import us.kbase.narrativemethodstore.FloatSliderOptions;
import us.kbase.narrativemethodstore.Icon;
import us.kbase.narrativemethodstore.IntSliderOptions;
import us.kbase.narrativemethodstore.MethodBehavior;
import us.kbase.narrativemethodstore.MethodBriefInfo;
import us.kbase.narrativemethodstore.MethodFullInfo;
import us.kbase.narrativemethodstore.MethodParameter;
import us.kbase.narrativemethodstore.MethodSpec;
import us.kbase.narrativemethodstore.OutputMapping;
import us.kbase.narrativemethodstore.Publication;
import us.kbase.narrativemethodstore.RadioOptions;
import us.kbase.narrativemethodstore.RegexMatcher;
import us.kbase.narrativemethodstore.ScreenShot;
import us.kbase.narrativemethodstore.ScriptInputMapping;
import us.kbase.narrativemethodstore.ScriptOutputMapping;
import us.kbase.narrativemethodstore.ServiceMethodInputMapping;
import us.kbase.narrativemethodstore.ServiceMethodOutputMapping;
import us.kbase.narrativemethodstore.SubdataSelection;
import us.kbase.narrativemethodstore.Suggestions;
import us.kbase.narrativemethodstore.TabOptions;
import us.kbase.narrativemethodstore.TextAreaOptions;
import us.kbase.narrativemethodstore.TextOptions;
import us.kbase.narrativemethodstore.TextSubdataOptions;
import us.kbase.narrativemethodstore.WidgetSpec;
import us.kbase.narrativemethodstore.exceptions.NarrativeMethodStoreException;

public class NarrativeMethodData {
	protected String methodId;
	protected MethodBriefInfo briefInfo;
	protected MethodFullInfo fullInfo;
	protected MethodSpec methodSpec;
	
	public NarrativeMethodData(String methodId, JsonNode spec, Map<String, Object> display,
			FileLookup lookup) throws NarrativeMethodStoreException {
		try {
			update(methodId, spec, display, lookup);
		} catch (Throwable ex) {
			if (briefInfo.getName() == null)
				briefInfo.withName(briefInfo.getId());
			if (briefInfo.getCategories() == null)
				briefInfo.withCategories(Arrays.asList("error"));
			NarrativeMethodStoreException ret = new NarrativeMethodStoreException(ex.getMessage(), ex);
			ret.setErrorMethod(briefInfo);
			throw ret;
		}
	}
	
	public MethodBriefInfo getMethodBriefInfo() {
		return briefInfo;
	}
	
	public MethodFullInfo getMethodFullInfo() {
		return fullInfo;
	}
	
	public MethodSpec getMethodSpec() {
		return methodSpec;
	}
	
	
	public void update(String methodId, JsonNode spec, Map<String, Object> display,
			FileLookup lookup) throws NarrativeMethodStoreException {
		this.methodId = methodId;

		briefInfo = new MethodBriefInfo()
							.withId(this.methodId);

		List <String> categories = new ArrayList<String>(1);
		JsonNode cats = get(spec, "categories");
		for(int k=0; k<cats.size(); k++) {
			categories.add(cats.get(k).asText());
		}
		briefInfo.withCategories(categories);
		
		String methodName = getDisplayProp(display, "name", lookup);
		briefInfo.withName(methodName);
		String methodTooltip = getDisplayProp(display, "tooltip", lookup);
		briefInfo.withTooltip(methodTooltip);
		
		String methodSubtitle = methodTooltip;
		try {
			methodSubtitle = getDisplayProp(display, "subtitle", lookup);
		} catch (IllegalStateException e) { }
		briefInfo.withSubtitle(methodSubtitle);
		
		String methodDescription = getDisplayProp(display, "description", lookup);
		String methodTechnicalDescr = "";
		try { methodTechnicalDescr = getDisplayProp(display, "technical-description", lookup); }
		catch (IllegalStateException e) { /*tech description is optional; do nothing*/ }
		
		// if replacement text is missing, do nothing, we just won't have any replacement text
		String replacementText = null;
		try { replacementText = getDisplayProp(display,"replacement-text",lookup); }
		catch (IllegalStateException e) { }
		
		briefInfo.withVer(get(spec, "ver").asText());
		
		List <String> authors = jsonListToStringList(spec.get("authors"));

		List <String> kbContributors = jsonListToStringList(spec.get("kb_contributors"));
		
		List<ScreenShot> screenshots = new ArrayList<ScreenShot>();
		@SuppressWarnings("unchecked")
		List<String> imageNames = (List<String>)getDisplayProp("/", display, "screenshots");
		if (imageNames != null) {
			for (String imageName : imageNames)
				screenshots.add(new ScreenShot().withUrl("img?method_id=" + this.methodId + "&image_name=" + imageName));
		}
		
		@SuppressWarnings("unchecked")
		Icon icon = null;
		try {
			String iconName = getDisplayProp(display,"icon",lookup);
			if(iconName.trim().length()>0) {
				icon = new Icon().withUrl("img?method_id=" + this.methodId + "&image_name=" + iconName);
			}
			briefInfo.withIcon(icon);
		} catch (IllegalStateException e) { /* icon is optional, do nothing */ }
		
		List<Publication> publications = new ArrayList<Publication>();
		try {
			@SuppressWarnings("unchecked")
			List<Object> pubInfoList = (List<Object>)getDisplayProp("/", display, "publications");
			if (pubInfoList != null) {
				for (Object pubInfoObj : pubInfoList) {
					@SuppressWarnings("unchecked")
					Map<String,Object> pubInfoMap = (Map<String,Object>)pubInfoObj;
					boolean shouldAdd = false;
					Publication p = new Publication();
					if(pubInfoMap.get("pmid")!=null) { p.setPmid(pubInfoMap.get("pmid").toString()); shouldAdd = true; }
					if(pubInfoMap.get("link")!=null) { p.setLink(pubInfoMap.get("link").toString()); shouldAdd = true;}
					
					if(pubInfoMap.get("display-text")!=null) { p.setDisplayText(pubInfoMap.get("display-text").toString()); shouldAdd = true;}
					else if(shouldAdd) { 
						if(p.getLink()!=null) { p.setDisplayText(p.getLink()); }
						else if(p.getPmid()!=null) { p.setDisplayText(p.getPmid()); }
					}
					if(shouldAdd) {
						publications.add(p);
					}
				}
			}
		} catch(IllegalStateException e) {}
		
		List<String> relatedApps = new ArrayList<String>();
		List<String> nextApps = new ArrayList<String>();
		List<String> relatedMethods = new ArrayList<String>();
		List<String> nextMethods = new ArrayList<String>();
		try {
			@SuppressWarnings("unchecked")
			Map<String,Object> sugg = (Map<String,Object>)getDisplayProp("/", display, "suggestions");
			if(sugg.get("apps")!=null) {
				@SuppressWarnings("unchecked")
				Map<String,List<String>> suggApps = (Map<String, List<String>>) sugg.get("apps");
				if(suggApps.get("related")!=null) {
					relatedApps = suggApps.get("related");
				}
				if(suggApps.get("next")!=null) {
					nextApps = suggApps.get("next");
				}
			}
			if(sugg.get("methods")!=null) {
				@SuppressWarnings("unchecked")
				Map<String,List<String>> suggMethods = (Map<String, List<String>>) sugg.get("methods");
				if(suggMethods.get("related")!=null) {
					relatedMethods = suggMethods.get("related");
				}
				if(suggMethods.get("next")!=null) {
					nextMethods = suggMethods.get("next");
				}
			}
		} catch(IllegalStateException e) {}
		Suggestions suggestions = new Suggestions()
									.withRelatedApps(relatedApps)
									.withNextApps(nextApps)
									.withRelatedMethods(relatedMethods)
									.withNextMethods(nextMethods);
		
		fullInfo = new MethodFullInfo()
							.withId(this.methodId)
							.withName(methodName)
							.withVer(briefInfo.getVer())
							.withSubtitle(methodSubtitle)
							.withTooltip(methodTooltip)
							.withCategories(categories)
							
							.withAuthors(authors)
							.withKbContributors(kbContributors)
							.withContact(get(spec, "contact").asText())
							
							.withDescription(methodDescription)
							.withTechnicalDescription(methodTechnicalDescr)
							.withScreenshots(screenshots)
							.withIcon(icon)
		
							.withSuggestions(suggestions)
							
							.withPublications(publications);
		
		JsonNode widgetsNode = get(spec, "widgets");
		WidgetSpec widgets = new WidgetSpec()
							.withInput(getTextOrNull(widgetsNode.get("input")))
							.withOutput(getTextOrNull(widgetsNode.get("output")));
		JsonNode behaviorNode = get(spec, "behavior");
		MethodBehavior behavior = new MethodBehavior()
							.withPythonClass(getTextOrNull(behaviorNode.get("python_class")))
							.withPythonFunction(getTextOrNull(behaviorNode.get("python_function")));
		if (behaviorNode.get("service-mapping") != null) {
			JsonNode serviceMappingNode = behaviorNode.get("service-mapping");
			JsonNode paramsMappingNode = get("behavior/service-mapping", serviceMappingNode, "input_mapping");
			List<ServiceMethodInputMapping> paramsMapping = new ArrayList<ServiceMethodInputMapping>();
			for (int j = 0; j < paramsMappingNode.size(); j++) {
				JsonNode paramMappingNode = paramsMappingNode.get(j);
				String path = "behavior/service-mapping/input_mapping/" + j;
				ServiceMethodInputMapping paramMapping = new ServiceMethodInputMapping();
				for (Iterator<String> it2 = paramMappingNode.fieldNames(); it2.hasNext(); ) {
					String field = it2.next();
					if (field.equals("target_argument_position")) {
						paramMapping.withTargetArgumentPosition(getLongOrNull(paramMappingNode.get(field)));
					} else if (field.equals("target_property")) {
						paramMapping.withTargetProperty(getTextOrNull(paramMappingNode.get(field)));
					} else if (field.equals("target_type_transform")) {
						paramMapping.withTargetTypeTransform(getTextOrNull(paramMappingNode.get(field)));
					} else if (field.equals("input_parameter")) {
						paramMapping.withInputParameter(paramMappingNode.get(field).asText());
					} else if (field.equals("narrative_system_variable")) {
						paramMapping.withNarrativeSystemVariable(paramMappingNode.get(field).asText());
					} else if (field.equals("constant_value")) {
						paramMapping.withConstantValue(new UObject(paramMappingNode.get(field)));
					} else if (field.equals("generated_value")) {
						JsonNode generNode = paramMappingNode.get("generated_value");
						AutoGeneratedValue agv = new AutoGeneratedValue();
						for (Iterator<String> it3 = generNode.fieldNames(); it3.hasNext(); ) {
							String field3 = it3.next();
							if (field3.equals("prefix")) {
								agv.withPrefix(generNode.get(field3).asText());
							} else if (field3.equals("symbols")) {
								agv.withSymbols(generNode.get(field3).asLong());
							} else if (field3.equals("suffix")) {
								agv.withSuffix(generNode.get(field3).asText());
							} else {
								throw new IllegalStateException("Unknown field [" + field + "] in generated " +
										"value structure within path behavior/service-mapping/input_mapping/" + j + 
										"/generated_value");
							}
							paramMapping.withGeneratedValue(agv);
						}
					} else {
						throw new IllegalStateException("Unknown field [" + field + "] in method parameter " +
								"mapping structure within path " + path);
					}
				}
				paramsMapping.add(paramMapping);
			}
			List<ServiceMethodOutputMapping> outputMapping = new ArrayList<ServiceMethodOutputMapping>();
			JsonNode outputMappingNode = get("behavior/service-mapping", serviceMappingNode, "output_mapping");
			for (int j = 0; j < outputMappingNode.size(); j++) {
				JsonNode paramMappingNode = outputMappingNode.get(j);
				String path = "behavior/service-mapping/output_mapping/" + j;
				ServiceMethodOutputMapping paramMapping = new ServiceMethodOutputMapping();
				for (Iterator<String> it2 = paramMappingNode.fieldNames(); it2.hasNext(); ) {
					String field = it2.next();
					if (field.equals("target_property")) {
						paramMapping.withTargetProperty(getTextOrNull(paramMappingNode.get(field)));
					} else if (field.equals("target_type_transform")) {
						paramMapping.withTargetTypeTransform(getTextOrNull(paramMappingNode.get(field)));
					} else if (field.equals("input_parameter")) {
						paramMapping.withInputParameter(paramMappingNode.get(field).asText());
					} else if (field.equals("narrative_system_variable")) {
						paramMapping.withNarrativeSystemVariable(paramMappingNode.get(field).asText());
					} else if (field.equals("constant_value")) {
						paramMapping.withConstantValue(new UObject(paramMappingNode.get(field)));
					} else if (field.equals("service_method_output_path")) {
						paramMapping.withServiceMethodOutputPath(jsonListToStringList(paramMappingNode.get(field)));
					} else {
						throw new IllegalStateException("Unknown field [" + field + "] in method output " +
								"mapping structure within path " + path);
					}
				}
				outputMapping.add(paramMapping);
			}
			behavior
				.withKbServiceUrl(getTextOrNull(get("behavior/service-mapping", serviceMappingNode, "url")))
				.withKbServiceName(getTextOrNull(serviceMappingNode.get("name")))
				.withKbServiceMethod(getTextOrNull(get("behavior/service-mapping", serviceMappingNode, "method")))
				.withKbServiceInputMapping(paramsMapping)
				.withKbServiceOutputMapping(outputMapping);
		} else if (behaviorNode.get("none") != null) {
			JsonNode noneNode = behaviorNode.get("none");
			List<OutputMapping> outputMapping = new ArrayList<OutputMapping>();
			JsonNode outputMappingNode = get("behavior/none", noneNode, "output_mapping");
			for (int j = 0; j < outputMappingNode.size(); j++) {
				JsonNode paramMappingNode = outputMappingNode.get(j);
				String path = "behavior/none/output_mapping/" + j;
				OutputMapping paramMapping = new OutputMapping();
				for (Iterator<String> it2 = paramMappingNode.fieldNames(); it2.hasNext(); ) {
					String field = it2.next();
					if (field.equals("target_property")) {
						paramMapping.withTargetProperty(getTextOrNull(paramMappingNode.get(field)));
					} else if (field.equals("target_type_transform")) {
						paramMapping.withTargetTypeTransform(getTextOrNull(paramMappingNode.get(field)));
					} else if (field.equals("input_parameter")) {
						paramMapping.withInputParameter(paramMappingNode.get(field).asText());
					} else if (field.equals("narrative_system_variable")) {
						paramMapping.withNarrativeSystemVariable(paramMappingNode.get(field).asText());
					} else if (field.equals("constant_value")) {
						paramMapping.withConstantValue(new UObject(paramMappingNode.get(field)));
					} else {
						throw new IllegalStateException("Unknown field [" + field + "] in method output " +
								"mapping structure within path " + path);
					}
				}
				outputMapping.add(paramMapping);
			}
			behavior.withOutputMapping(outputMapping);
		} else if (behaviorNode.get("script-mapping") != null) {
			JsonNode scriptNode = behaviorNode.get("script-mapping");
			JsonNode paramsMappingNode = get("behavior/script-mapping", scriptNode, "input_mapping");
			List<ScriptInputMapping> paramsMapping = new ArrayList<ScriptInputMapping>();
			for (int j = 0; j < paramsMappingNode.size(); j++) {
				JsonNode paramMappingNode = paramsMappingNode.get(j);
				String path = "behavior/script-mapping/input_mapping/" + j;
				ScriptInputMapping paramMapping = new ScriptInputMapping();
				for (Iterator<String> it2 = paramMappingNode.fieldNames(); it2.hasNext(); ) {
					String field = it2.next();
					if (field.equals("target_property")) {
						paramMapping.withTargetProperty(getTextOrNull(paramMappingNode.get(field)));
					} else if (field.equals("target_type_transform")) {
						paramMapping.withTargetTypeTransform(getTextOrNull(paramMappingNode.get(field)));
					} else if (field.equals("input_parameter")) {
						paramMapping.withInputParameter(paramMappingNode.get(field).asText());
					} else if (field.equals("narrative_system_variable")) {
						paramMapping.withNarrativeSystemVariable(paramMappingNode.get(field).asText());
					} else if (field.equals("constant_value")) {
						paramMapping.withConstantValue(new UObject(paramMappingNode.get(field)));
					} else if (field.equals("generated_value")) {
						JsonNode generNode = paramMappingNode.get("generated_value");
						AutoGeneratedValue agv = new AutoGeneratedValue();
						for (Iterator<String> it3 = generNode.fieldNames(); it3.hasNext(); ) {
							String field3 = it3.next();
							if (field3.equals("prefix")) {
								agv.withPrefix(generNode.get(field3).asText());
							} else if (field3.equals("symbols")) {
								agv.withSymbols(generNode.get(field3).asLong());
							} else if (field3.equals("suffix")) {
								agv.withSuffix(generNode.get(field3).asText());
							} else {
								throw new IllegalStateException("Unknown field [" + field + "] in generated " +
										"value structure within path behavior/script-mapping/input_mapping/" + j + 
										"/generated_value");
							}
							paramMapping.withGeneratedValue(agv);
						}
					} else {
						throw new IllegalStateException("Unknown field [" + field + "] in method parameter " +
								"mapping structure within path " + path);
					}
				}
				paramsMapping.add(paramMapping);
			}
			List<ScriptOutputMapping> outputMapping = new ArrayList<ScriptOutputMapping>();
			JsonNode outputMappingNode = get("behavior/script-mapping", scriptNode, "output_mapping");
			for (int j = 0; j < outputMappingNode.size(); j++) {
				JsonNode paramMappingNode = outputMappingNode.get(j);
				String path = "behavior/script-mapping/output_mapping/" + j;
				ScriptOutputMapping paramMapping = new ScriptOutputMapping();
				for (Iterator<String> it2 = paramMappingNode.fieldNames(); it2.hasNext(); ) {
					String field = it2.next();
					if (field.equals("target_property")) {
						paramMapping.withTargetProperty(getTextOrNull(paramMappingNode.get(field)));
					} else if (field.equals("target_type_transform")) {
						paramMapping.withTargetTypeTransform(getTextOrNull(paramMappingNode.get(field)));
					} else if (field.equals("input_parameter")) {
						paramMapping.withInputParameter(paramMappingNode.get(field).asText());
					} else if (field.equals("narrative_system_variable")) {
						paramMapping.withNarrativeSystemVariable(paramMappingNode.get(field).asText());
					} else if (field.equals("constant_value")) {
						paramMapping.withConstantValue(new UObject(paramMappingNode.get(field)));
					} else if (field.equals("script_output_path")) {
						paramMapping.withScriptOutputPath(jsonListToStringList(paramMappingNode.get(field)));
					} else {
						throw new IllegalStateException("Unknown field [" + field + "] in method output " +
								"mapping structure within path " + path);
					}
				}
				outputMapping.add(paramMapping);
			}
			JsonNode hasFiles = scriptNode.get("has_files");
			behavior
				.withScriptModule(getTextOrNull(get("behavior/script-mapping", scriptNode, "module")))
				.withScriptName(getTextOrNull(get("behavior/script-mapping", scriptNode, "script")))
				.withScriptHasFiles(hasFiles == null ? 0L : jsonBooleanToRPC(hasFiles))
				.withScriptInputMapping(paramsMapping)
				.withScriptOutputMapping(outputMapping);
		}
		List<MethodParameter> parameters = new ArrayList<MethodParameter>();
		JsonNode parametersNode = get(spec, "parameters");
		@SuppressWarnings("unchecked")
		Map<String, Object> paramsDisplays = (Map<String, Object>)getDisplayProp("/", display, "parameters");
		Set<String> paramIds = new TreeSet<String>();
		for (int i = 0; i < parametersNode.size(); i++) {
			JsonNode paramNode = parametersNode.get(i);
			String paramPath = "parameters/" + i;
			String paramId = get(paramPath, paramNode, "id").asText();
			String uiClass = "parameter";
			paramIds.add(paramId);
			@SuppressWarnings("unchecked")
			Map<String, Object> paramDisplay = (Map<String, Object>)getDisplayProp("parameters", paramsDisplays, paramId);
			TextOptions textOpt = null;
			if (paramNode.has("text_options")) {
				JsonNode optNode = get(paramPath, paramNode, "text_options");
				JsonNode isOutputName = optNode.get("is_output_name");
				long isOutputNameFlag = 0L;
				
				if(isOutputName!=null) {
					if(isOutputName.asBoolean()){
						isOutputNameFlag = 1L;
					}
				}
				String placeholder = "";
				try {
					placeholder = (String) getDisplayProp("parameters/" + paramId, paramDisplay, "placeholder");
				} catch (IllegalStateException e) { }
				
				textOpt = new TextOptions()
							.withValidWsTypes(jsonListToStringList(optNode.get("valid_ws_types")))
							.withValidateAs(getTextOrNull(optNode.get("validate_as")))
							.withIsOutputName(isOutputNameFlag)
							.withPlaceholder(placeholder);
				if(textOpt.getValidWsTypes() != null) {
					if(textOpt.getValidWsTypes().size()>0) {
						uiClass = "input";
						if(isOutputNameFlag == 1L) {
							uiClass = "output";
						}
					}
				}
				
				// todo: add better checks of min/max numbers, like if it is numeric
				if(optNode.get("min_int")!=null) {
					textOpt.withMinInt(optNode.get("min_int").asLong());
				}
				if(optNode.get("max_int")!=null) {
					textOpt.withMaxInt(optNode.get("max_int").asLong());
				}
				if(optNode.get("min_float")!=null) {
					textOpt.withMinFloat(optNode.get("min_float").asDouble());
				}
				if(optNode.get("max_float")!=null) {
					textOpt.withMaxFloat(optNode.get("max_float").asDouble());
				}
				
				List<RegexMatcher> regexList = new ArrayList<RegexMatcher>();
				if(optNode.get("regex_constraint")!=null) {
					for(int rxi=0; rxi<optNode.get("regex_constraint").size(); rxi++) {
						JsonNode regex = optNode.get("regex_constraint").get(rxi);
						if(regex.get("regex")!=null && regex.get("error_text")!=null) {
							Long match = new Long(1);
							if(regex.get("match")!=null) {
								if(regex.get("match").asBoolean()) {
									match = new Long(1);
								} else {
									match = new Long(0);
								}
							}
							regexList.add(
									new RegexMatcher()
										.withMatch(match)
										.withRegex(regex.get("regex").asText())
										.withErrorText(regex.get("error_text").asText()));
						}
					}
				}
				textOpt.withRegexConstraint(regexList);
			}
			TextSubdataOptions textSubdataOpt = null;
			if (paramNode.has("textsubdata_options")) {
				JsonNode optNode = get(paramPath, paramNode, "textsubdata_options");
				
				// multiselection - default is false
				JsonNode multiselection = optNode.get("multiselection");
				long multiselectionFlag = 0L;
				if(multiselection!=null) {
					if(multiselection.asBoolean()){
						multiselectionFlag = 1L;
					}
				}
				// show_src_obj - default is true
				JsonNode show_src_obj = optNode.get("show_src_obj");
				long show_src_objFlag = 1L;
				if(show_src_obj!=null) {
					if(!show_src_obj.asBoolean()){
						show_src_objFlag = 0L;
					}
				}
				// allow_custom - default is false
				JsonNode allow_custom = optNode.get("allow_custom");
				long allow_customFlag = 0L;
				if(allow_custom!=null) {
					if(allow_custom.asBoolean()){
						allow_customFlag = 1L;
					}
				}
				
				String placeholder = "";
				try {
					placeholder = (String) getDisplayProp("parameters/" + paramId, paramDisplay, "placeholder");
				} catch (IllegalStateException e) { }
				
				JsonNode subdataSelection = optNode.get("subdata_selection");
				if(subdataSelection==null) {
					throw new IllegalStateException("In parameter [" + paramId + "] has textsubdata_options  " +
							"without a subdata selection defined");
				}
				List <String> pathToSubdata = jsonListToStringList(subdataSelection.get("path_to_subdata"));
				if(pathToSubdata==null) pathToSubdata = new ArrayList<String>();
				List <String> subdata_included = jsonListToStringList(subdataSelection.get("subdata_included"));
				if(subdata_included==null) subdata_included = new ArrayList<String>();
				SubdataSelection ss = new SubdataSelection()
											.withConstantRef(jsonListToStringList(subdataSelection.get("constant_ref")))
											.withParameterId(getTextOrNull(subdataSelection.get("parameter_id")))
											.withSubdataIncluded(subdata_included)
											.withPathToSubdata(pathToSubdata)
											.withSelectionId(getTextOrNull(subdataSelection.get("selection_id")))
											.withSelectionDescription(jsonListToStringList(subdataSelection.get("selection_description")))
											.withDescriptionTemplate(getTextOrNull(subdataSelection.get("description_template")));
				
				textSubdataOpt = new TextSubdataOptions()
										.withPlaceholder(placeholder)
										.withMultiselection(multiselectionFlag)
										.withShowSrcObj(show_src_objFlag)
										.withAllowCustom(allow_customFlag)
										.withSubdataSelection(ss);
				// TODO: add more validation here, like if the parameter id is valid, or if there were extra fields
				// that weren't allowed, rather than just setting things to null if they don't exist
			}
			CheckboxOptions cbOpt = null;
			if (paramNode.has("checkbox_options")) {
				JsonNode optNode = get(paramPath, paramNode, "checkbox_options");
				long checkedValue = get(paramPath + "/checkbox_options", optNode, "checked_value").asLong();
				long uncheckedValue = get(paramPath + "/checkbox_options", optNode, "unchecked_value").asLong();
				cbOpt = new CheckboxOptions().withCheckedValue(checkedValue).withUncheckedValue(uncheckedValue);
			}
			DropdownOptions ddOpt = null;
			if (paramNode.has("dropdown_options")) {
				JsonNode optNode = get(paramPath, paramNode, "dropdown_options");
				optNode = get(paramPath + "/dropdown_options", optNode, "options");
				List<DropdownOption> options = new ArrayList<DropdownOption>();
				for (int j = 0; j < optNode.size(); j++) {
					JsonNode itemNode = optNode.get(j);
					String value = get(paramPath + "/dropdown_options/options/" + j, itemNode, "value").asText();
					String displayText = get(paramPath + "/dropdown_options/options/" + j, itemNode, "display").asText();
					options.add(new DropdownOption().withValue(value).withDisplay(displayText));
				}
				ddOpt = new DropdownOptions().withOptions(options);
			}
			FloatSliderOptions floatOpt = null;
			if (paramNode.has("floatslider_options")) {
				JsonNode optNode = get(paramPath, paramNode, "floatslider_options");
				double min = get(paramPath + "/floatslider_options", optNode, "min").asDouble();
				double max = get(paramPath + "/floatslider_options", optNode, "max").asDouble();
				floatOpt = new FloatSliderOptions().withMin(min).withMax(max);
			}
			IntSliderOptions intOpt = null;
			if (paramNode.has("intslider_options")) {
				JsonNode optNode = get(paramPath, paramNode, "intslider_options");
				long min = get(paramPath + "/intslider_options", optNode, "min").asLong();
				long max = get(paramPath + "/intslider_options", optNode, "max").asLong();
				long step = get(paramPath + "/intslider_options", optNode, "step").asLong();
				intOpt = new IntSliderOptions().withMin(min).withMax(max).withStep(step);
			}
			RadioOptions radioOpt = null;
			if (paramNode.has("radio_options")) {
				JsonNode optNode = get(paramPath, paramNode, "radio_options");
				optNode = get(paramPath + "/radio_options", optNode, "options");
				List<String> idOrder = new ArrayList<String>();
				Map<String, String> options = new LinkedHashMap<String, String>();
				Map<String, String> tooltips = new LinkedHashMap<String, String>();
				for (int j = 0; j < optNode.size(); j++) {
					JsonNode itemNode = optNode.get(j);
					String id = get(paramPath + "/radio_options/options/" + j, itemNode, "id").asText();
					String uiName = get(paramPath + "/radio_options/options/" + j, itemNode, "ui_name").asText();
					String uiTooltip = get(paramPath + "/radio_options/options/" + j, itemNode, "ui_tooltip").asText();
					idOrder.add(id);
					options.put(id, uiName);
					tooltips.put(id, uiTooltip);
				}
				radioOpt = new RadioOptions().withIdOrder(idOrder).withIdsToOptions(options).withIdsToTooltip(tooltips);
			}
			TextAreaOptions taOpt = null;
			if (paramNode.has("textarea_options")) {
				JsonNode optNode = get(paramPath, paramNode, "textarea_options");
				long nRows = get(paramPath + "/textarea_options", optNode, "n_rows").asLong();
				String placeholder = "";
				try {
					placeholder = (String) getDisplayProp("parameters/" + paramId, paramDisplay, "placeholder");
				} catch (IllegalStateException e) { }
				
				List<RegexMatcher> regexList = new ArrayList<RegexMatcher>();
				if(optNode.get("regex_constraint")!=null) {
					for(int rxi=0; rxi<optNode.get("regex_constraint").size(); rxi++) {
						JsonNode regex = optNode.get("regex_constraint").get(rxi);
						if(regex.get("regex")!=null && regex.get("error_text")!=null) {
							Long match = new Long(1);
							if(regex.get("match")!=null) {
								if(regex.get("match").asBoolean()) {
									match = new Long(1);
								} else {
									match = new Long(0);
								}
							}
							regexList.add(
									new RegexMatcher()
										.withMatch(match)
										.withRegex(regex.get("regex").asText())
										.withErrorText(regex.get("error_text").asText()));
						}
					}
				}
				
				taOpt = new TextAreaOptions()
								.withNRows(nRows)
								.withPlaceholder(placeholder)
								.withRegexConstraint(regexList);
			}
			TabOptions tabOpt = null;
			if (paramNode.has("tab_options")) {
				JsonNode optNode = get(paramPath, paramNode, "tab_options");
				optNode = get(paramPath + "/tab_options", optNode, "options");
				List<String> idOrder = new ArrayList<String>();
				Map<String, String> options = new LinkedHashMap<String, String>();
				Map<String, List<String>> tabIdToParamIds = new LinkedHashMap<String, List<String>>();
				for (int j = 0; j < optNode.size(); j++) {
					JsonNode itemNode = optNode.get(j);
					String id = get(paramPath + "/tab_options/options/" + j, itemNode, "id").asText();
					idOrder.add(id);
					String uiName = get(paramPath + "/tab_options/options/" + j, itemNode, "ui_name").asText();
					options.put(id, uiName);
					List<String> tabParamIds = jsonListToStringList(itemNode.get("param_ids"));
					if (tabParamIds != null) 
						tabIdToParamIds.put(id, tabParamIds);
				}
				tabOpt = new TabOptions().withTabIdOrder(idOrder).withTabIdToTabName(options).withTabIdToParamIds(tabIdToParamIds);
			}
			
			String paramDescription = "";
			try {
				paramDescription = (String) getDisplayProp("parameters/" + paramId, paramDisplay, "long-hint");
			} catch (IllegalStateException e) { }
			try {
				paramDescription = (String)getDisplayProp("parameters/" + paramId, paramDisplay, "description");
			} catch (IllegalStateException e) {}
			Long disabled = 0L;
			try {
				disabled = jsonBooleanToRPC(get(paramPath, paramNode, "disabled"));
			} catch (IllegalStateException e) {}
			
			
			MethodParameter param = new MethodParameter()
							.withId(paramId)
							.withUiName((String)getDisplayProp("parameters/" + paramId, paramDisplay, "ui-name"))
							.withShortHint((String)getDisplayProp("parameters/" + paramId, paramDisplay, "short-hint"))
							.withDescription(paramDescription)
							.withOptional(jsonBooleanToRPC(get(paramPath, paramNode, "optional")))
							.withAdvanced(jsonBooleanToRPC(get(paramPath, paramNode, "advanced")))
							.withDisabled(disabled)
							.withUiClass(uiClass)
							.withAllowMultiple(jsonBooleanToRPC(get(paramPath, paramNode, "allow_multiple")))
							.withDefaultValues(jsonListToStringList(get(paramPath, paramNode, "default_values")))
							.withFieldType(get(paramPath, paramNode, "field_type").asText())
							.withTextOptions(textOpt)
							.withTextsubdataOptions(textSubdataOpt)
							.withCheckboxOptions(cbOpt)
							.withDropdownOptions(ddOpt)
							.withFloatsliderOptions(floatOpt)
							.withIntsliderOptions(intOpt)
							.withRadioOptions(radioOpt)
							.withTextareaOptions(taOpt)
							.withTabOptions(tabOpt);
			parameters.add(param);
		}
		
		List<FixedMethodParameter> fixedParameters = new ArrayList<FixedMethodParameter>();
		try {
			@SuppressWarnings("unchecked")
			List<Object> fixedParams = (List<Object>)getDisplayProp("/", display, "fixed-parameters");
			for (int i = 0; i < fixedParams.size(); i++) {
				@SuppressWarnings("unchecked")
				Map<String,String> fixedParam = (Map<String, String>) fixedParams.get(i);
				String fmpName = "";
				if(fixedParam.get("ui-name")!=null) {
					fmpName = fixedParam.get("ui-name").trim();
				}
				String fmpDescription = "";
				if(fixedParam.get("description")!=null) {
					fmpDescription = fixedParam.get("description").trim();
				}
				FixedMethodParameter fmp = new FixedMethodParameter()
												.withUiName(fmpName)
												.withDescription(fmpDescription);
				fixedParameters.add(fmp);
			}
		} catch (IllegalStateException e) { /* fixed parameters are optional; do nothing */ }
		
		if (behavior.getKbServiceInputMapping() != null) {
			for (int i = 0; i < behavior.getKbServiceInputMapping().size(); i++) {
				ServiceMethodInputMapping mapping = behavior.getKbServiceInputMapping().get(i);
				String paramId = mapping.getInputParameter();
				if (paramId != null && !paramIds.contains(paramId)) {
					throw new IllegalStateException("Undeclared parameter [" + paramId + "] found " +
							"within path [behavior/service-mapping/input_mapping/" + i + "]");
				}
			}
		}
		if (behavior.getScriptInputMapping() != null) {
			for (int i = 0; i < behavior.getScriptInputMapping().size(); i++) {
				ScriptInputMapping mapping = behavior.getScriptInputMapping().get(i);
				String paramId = mapping.getInputParameter();
				if (paramId != null && !paramIds.contains(paramId)) {
					throw new IllegalStateException("Undeclared parameter [" + paramId + "] found " +
							"within path [behavior/script-mapping/input_mapping/" + i + "]");
				}
			}
		}
		if (behavior.getKbServiceOutputMapping() != null) {
			for (int i = 0; i < behavior.getKbServiceOutputMapping().size(); i++) {
				ServiceMethodOutputMapping mapping = behavior.getKbServiceOutputMapping().get(i);
				String paramId = mapping.getInputParameter();
				if (paramId != null && !paramIds.contains(paramId)) {
					throw new IllegalStateException("Undeclared parameter [" + paramId + "] found " +
							"within path [behavior/service-mapping/output_mapping/" + i + "]");
				}
			}
		}
		if (behavior.getOutputMapping() != null) {
			for (int i = 0; i < behavior.getOutputMapping().size(); i++) {
				OutputMapping mapping = behavior.getOutputMapping().get(i);
				String paramId = mapping.getInputParameter();
				if (paramId != null && !paramIds.contains(paramId)) {
					throw new IllegalStateException("Undeclared parameter [" + paramId + "] found " +
							"within path [behavior/none/output_mapping/" + i + "]");
				}
			}
		}
		if (behavior.getScriptOutputMapping() != null) {
			for (int i = 0; i < behavior.getScriptOutputMapping().size(); i++) {
				ScriptOutputMapping mapping = behavior.getScriptOutputMapping().get(i);
				String paramId = mapping.getInputParameter();
				if (paramId != null && !paramIds.contains(paramId)) {
					throw new IllegalStateException("Undeclared parameter [" + paramId + "] found " +
							"within path [behavior/script-mapping/output_mapping/" + i + "]");
				}
			}
		}
		
		methodSpec = new MethodSpec()
							.withInfo(briefInfo)
							.withReplacementText(replacementText)
							.withWidgets(widgets)
							.withBehavior(behavior)
							.withParameters(parameters)
							.withFixedParameters(fixedParameters)
							.withJobIdOutputField(getTextOrNull(spec.get("job_id_output_field")));
	}

	private static JsonNode get(JsonNode node, String childName) {
		return get(null, node, childName);
	}
	
	private static JsonNode get(String nodePath, JsonNode node, String childName) {
		JsonNode ret = node.get(childName);
		if (ret == null)
			throw new IllegalStateException("Can't find sub-node [" + childName + "] within " +
					"path [" + (nodePath == null ? "/" : nodePath) + "] in spec.json");
		return ret;
	}
	
	private static String getDisplayProp(Map<String, Object> display, String propName, 
			FileLookup lookup) {
		return getDisplayProp(display, propName, lookup, propName);
	}

	private static String getDisplayProp(Map<String, Object> display, String propName, 
			FileLookup lookup, String lookupName) {
		String ret = lookup.loadFileContent(lookupName + ".html");
		if (ret == null)
			ret = (String)getDisplayProp("/", display, propName);
		return ret;
	}
	
	private static Object getDisplayProp(String path, Map<String, Object> display, String propName) {
		Object ret = display.get(propName);
		if (ret == null)
			throw new IllegalStateException("Can't find property [" + propName + "] within path [" + 
					path + "] in display.yaml");
		return ret;
	}

	private static String getTextOrNull(JsonNode node) {
		return node == null ? null : node.asText();
	}

	private static Long getLongOrNull(JsonNode node) {
		return node == null || node.isNull() ? null : node.asLong();
	}

	private static Long jsonBooleanToRPC(JsonNode node) {
		return node.asBoolean() ? 1L : 0L;
	}
	
	private static List<String> jsonListToStringList(JsonNode node) {
		if (node == null)
			return null;
		List<String> ret = new ArrayList<String>();
		for (int i = 0; i < node.size(); i++)
			ret.add(node.get(i).asText());
		return ret;
	}

	private static Map<String, String> jsonMapToStringMap(JsonNode node) {
		if (node == null)
			return null;
		Map<String, String> ret = new LinkedHashMap<String, String>();
		for (Iterator<String> it = node.fieldNames(); it.hasNext(); ) {
			String key = it.next();
			ret.put(key, node.get(key).asText());
		}
		return ret;
	}
}
