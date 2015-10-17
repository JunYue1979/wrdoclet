package net.winroad.wrdoclet;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.winroad.wrdoclet.data.OpenAPI;
import net.winroad.wrdoclet.data.WRDoc;
import net.winroad.wrdoclet.doc.API;
import net.winroad.wrdoclet.doc.Doc;
import net.winroad.wrdoclet.doc.DocData;
import net.winroad.wrdoclet.taglets.WRMemoTaglet;
import net.winroad.wrdoclet.taglets.WRReturnCodeTaglet;
import net.winroad.wrdoclet.taglets.WRTagTaglet;
import net.winroad.wrdoclet.utils.Util;

import org.apache.commons.lang.StringUtils;

import com.google.gson.Gson;
import com.sun.javadoc.AnnotationTypeDoc;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.DocErrorReporter;
import com.sun.javadoc.RootDoc;
import com.sun.tools.doclets.internal.toolkit.Configuration;
import com.sun.tools.doclets.internal.toolkit.builders.AbstractBuilder;
import com.sun.tools.doclets.internal.toolkit.util.ClassTree;
import com.sun.tools.doclets.internal.toolkit.util.DocletAbortException;

/**
 * @author AdamsLee
 * 
 */
public class HtmlDoclet extends AbstractDoclet {

	public HtmlDoclet() {
		configuration = (ConfigurationImpl) configuration();
	}

	/**
	 * The global configuration information for this run.
	 */
	public ConfigurationImpl configuration;

	/**
	 * The "start" method as required by Javadoc.
	 * 
	 * @param root
	 *            the root of the documentation tree.
	 * @see com.sun.javadoc.RootDoc
	 * @return true if the doclet ran without encountering any errors.
	 */
	public static boolean start(RootDoc root) {
		try {
			HtmlDoclet doclet = new HtmlDoclet();
			return doclet.start(doclet, root);
		} finally {
			ConfigurationImpl.reset();
		}
	}

	@Override
	public Configuration configuration() {
		return ConfigurationImpl.getInstance();
	}

	protected DocData generateDocData(WRDoc wrDoc) {
		DocData result = new DocData();
		List<String> tagList = new ArrayList<String>(wrDoc.getWRTags());
		Collator cmp = Collator.getInstance(java.util.Locale.CHINA);
		Collections.sort(tagList, cmp);
		for (String tag : tagList) {
			result.getFacet_counts()
					.getFacet_fields()
					.addTagField(tag, wrDoc.getTaggedOpenAPIs().get(tag).size());
			Doc doc = new Doc();
			doc.setTag(tag);
			for (OpenAPI openAPI : wrDoc.getTaggedOpenAPIs().get(tag)) {
				API api = new API();
				api.setUrl(openAPI.getRequestMapping().getUrl());
				api.setTooltip(openAPI.getRequestMapping().getTooltip());
				api.setMethodType(openAPI.getRequestMapping().getMethodType());
				String filename = generateWRAPIFileName(openAPI
						.getRequestMapping().getUrl(), openAPI
						.getRequestMapping().getMethodType());
				api.setFilepath(filename);
				api.setBrief(openAPI.getBrief());
				doc.getAPIs().add(api);
			}
			result.addDoc(doc);
		}
		return result;
	}

	@Override
	protected void generateWRDocFiles(RootDoc root, WRDoc wrDoc)
			throws Exception {
		if (this.configuration.noframe) {
			this.generateWRNoFrameFile(root, wrDoc);
		} else {
			this.generateWRFrameFiles(root, wrDoc);
		}
	}

	protected void generateWRNoFrameFile(RootDoc root, WRDoc wrDoc)
			throws Exception {
		Map<String, Map<String, List<OpenAPI>>> tagMap = new HashMap<String, Map<String, List<OpenAPI>>>();
		tagMap.put("tagedOpenAPIs", wrDoc.getTaggedOpenAPIs());
		this.configuration
				.getWriterFactory()
				.getFreemarkerWriter()
				.generateHtmlFile(
						this.configuration.getFreemarkerTemplateFilePath(),
						"wrNoFrame.ftl", tagMap,
						this.configuration.destDirName, "index.html");
		this.generateCommonFiles(root);
	}

	protected void generateWRFrameFiles(RootDoc root, WRDoc wrDoc)
			throws Exception {
		this.generateIndexFile(root, wrDoc);
		this.generateWRAPIDetailFiles(root, wrDoc);
		this.generateCommonFiles(root);
	}

	protected void generateIndexFile(RootDoc root, WRDoc wrDoc)
			throws Exception {
		List<String> tagList = new ArrayList<String>(wrDoc.getWRTags());
		Collator cmp = Collator.getInstance(java.util.Locale.CHINA);
		Collections.sort(tagList, cmp);
		Map<String, Object> tagMap = new HashMap<String, Object>();
		DocData bean = this.generateDocData(wrDoc);
		Gson gson = new Gson();
		tagMap.put("response", gson.toJson(bean));
		tagMap.put("searchengine", this.configuration.searchengine);
		this.configuration
				.getWriterFactory()
				.getFreemarkerWriter()
				.generateHtmlFile(
						this.configuration.getFreemarkerTemplateFilePath(),
						"index.ftl", tagMap, this.configuration.destDirName,
						null);
	}

	protected void generateCommonFiles(RootDoc root) throws Exception {
		// copy folders
		Util.copyResourceFolder("/css/",
				Util.combineFilePath(this.configuration.destDirName, "css"));
		Util.copyResourceFolder("/js/",
				Util.combineFilePath(this.configuration.destDirName, "js"));
		Util.copyResourceFolder("/img/",
				Util.combineFilePath(this.configuration.destDirName, "img"));
	}

	protected String generateWRAPIFileName(String url, String methodType) {
		return StringUtils.strip(
				(url + '-' + methodType + '-').replace('/', '-')
						.replace('\\', '-').replace(':', '-').replace('*', '-')
						.replace('?', '-').replace('"', '-').replace('<', '-')
						.replace('>', '-').replace('|', '-').replace('{', '-')
						.replace('}', '-'), "-")
				+ ".html";
	}

	/**
	 * Generate the wr.tag documentation.
	 * 
	 * @param root
	 *            the RootDoc of source to document.
	 * @param wrDoc
	 *            the data structure representing the doc to generate.
	 */
	protected void generateWRAPIDetailFiles(RootDoc root, WRDoc wrDoc) {
		List<String> tagList = new ArrayList<String>(wrDoc.getWRTags());
		for (String tag : tagList) {
			List<OpenAPI> openAPIList = wrDoc.getTaggedOpenAPIs().get(tag);
			Set<String> filesGenerated = new HashSet<String>();
			for (OpenAPI openAPI : openAPIList) {
				Map<String, Object> hashMap = new HashMap<String, Object>();
				hashMap.put("openAPI", openAPI);
				String tagsStr = openAPI.getTags().toString();
				// trim '[' and ']'
				hashMap.put("tags", tagsStr.substring(1, tagsStr.length() - 1));
				hashMap.put("generatedTime", wrDoc.getDocGeneratedTime());
				hashMap.put(
						"branchName",
						((ConfigurationImpl) wrDoc.getConfiguration()).branchname);
				hashMap.put(
						"systemName",
						((ConfigurationImpl) wrDoc.getConfiguration()).systemname);
				String filename = generateWRAPIFileName(openAPI
						.getRequestMapping().getUrl(), openAPI
						.getRequestMapping().getMethodType());
				hashMap.put("filePath", filename);
				if (!filesGenerated.contains(filename)) {
					this.configuration
							.getWriterFactory()
							.getFreemarkerWriter()
							.generateHtmlFile(
									this.configuration
											.getFreemarkerTemplateFilePath(),
									"wrAPIDetail.ftl", hashMap,
									this.configuration.destDirName, filename);
					filesGenerated.add(filename);
				}
			}
		}
	}

	@Override
	protected void generateClassFiles(ClassDoc[] arr, ClassTree classtree) {

		Arrays.sort(arr);
		for (int i = 0; i < arr.length; i++) {
			if (!(configuration.isGeneratedDoc(arr[i]) && arr[i].isIncluded())) {
				continue;
			}
			ClassDoc prev = (i == 0) ? null : arr[i - 1];
			ClassDoc curr = arr[i];
			ClassDoc next = (i + 1 == arr.length) ? null : arr[i + 1];
			try {
				if (curr.isAnnotationType()) {
					AbstractBuilder annotationTypeBuilder = configuration
							.getBuilderFactory().getAnnotationTypeBuilder(
									(AnnotationTypeDoc) curr, prev, next);
					annotationTypeBuilder.build();
				} else {
					AbstractBuilder classBuilder = configuration
							.getBuilderFactory().getClassBuilder(curr, prev,
									next, classtree);
					classBuilder.build();
				}
			} catch (Exception e) {
				e.printStackTrace();
				throw new DocletAbortException();
			}
		}

	}

	@Override
	protected void generatePackageFiles(ClassTree arg0) throws Exception {

	}

	public static boolean validOptions(String options[][],
			DocErrorReporter reporter) {
		return (ConfigurationImpl.getInstance())
				.validOptions(options, reporter);
	}

	public static int optionLength(String option) {
		return (ConfigurationImpl.getInstance()).optionLength(option);
	}

	public static void main(String[] args) {
		String[] docArgs = new String[] {
				"-doclet",
				HtmlDoclet.class.getName(),
				"-docletpath",
				new File(System.getProperty("user.dir"), "target/classes")
						.getAbsolutePath(),
				"-taglet",
				WRTagTaglet.class.getName(),
				WRMemoTaglet.class.getName(),
				WRReturnCodeTaglet.class.getName(),
				"-tagletpath",
				new File(System.getProperty("user.dir"), "target/classes")
						.getAbsolutePath(),
				"-encoding",
				"utf-8",
				"-charset",
				"utf-8",
				"-sourcepath",
				"D:/Git/wrdoclet/wrdoclet-demosite/src/main/java",
				"net.winroad.Controller",
				"net.winroad.Models",
				"org.springframework.web.bind.annotation",
				"-classpath",
				new File(
						System.getProperty("user.home"),
						".m2/repository/org/springframework/spring-web/3.2.3.RELEASE/spring-web-3.2.3.RELEASE.jar")
						.getAbsolutePath(),
				"-d",
				new File(System.getProperty("user.dir"), "target/doc")
						.getAbsolutePath() };
		com.sun.tools.javadoc.Main.execute(docArgs);

		System.out.println("Doc generated to: "
				+ new File(System.getProperty("user.dir"), "target/doc")
						.getAbsolutePath());
	}

}
