package org.sakaiproject.component.impl;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.util.Xml;
import org.springframework.core.io.Resource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * This class manages the configuration for the tools.
 */
public class ToolConfigurationService {
    
    private final Log log = LogFactory.getLog(ToolConfigurationService.class);

    private boolean useToolGroup;
    /**
     * File name within sakai.home for the tool order file.
     */
    private String toolOrderFile = null;
    private Resource defaultToolOrderResource;
    /**
     * loaded tool orders - map keyed by category of List of tool id strings.
     */
    private Map m_toolOrders = new HashMap();
    private Map m_toolGroups = new HashMap<String, List>(); // Map = [group1,{tool1,tool2,tool3}],[group2,{tool2,tool4}],[group3,{tool1,tool5}]

    private Map m_toolGroupCategories = new HashMap<String,List>(); // Map = [course,{group1, group2,group3}],[project,{group1, group3, group4}],[portfolio,{group4}]

    private Map m_toolGroupRequired = new HashMap<String,List>();

    private Map m_toolGroupSelected = new HashMap<String,List>();

    private Map m_toolsRequired = new HashMap();
    /**
     * default tools - map keyed by category of List of tool id strings.
     */
    private Map m_defaultTools = new HashMap();
    /**
     * default tool categories in order mapped by site type
     */
    private Map<String, List<String>> m_toolCategoriesList = new HashMap<String, List<String>>();
    /**
     * default tool categories to tool id maps mapped by site type
     */
    private Map<String, Map<String, List<String>>> m_toolCategoriesMap = new HashMap<String, Map<String, List<String>>>();
    /**
     * default tool id to tool category maps mapped by site type
     */
    private Map<String, Map<String, String>> m_toolToToolCategoriesMap = new HashMap<String, Map<String, String>>();

    public ToolConfigurationService() {
    }

    void setUseToolGroup(boolean useToolGroup) {
        this.useToolGroup = useToolGroup;
    }

    void setToolOrderFile(String toolOrderFile) {
        this.toolOrderFile = toolOrderFile;
    }

    void setDefaultToolOrderResource(Resource defaultToolOrderResource) {
        this.defaultToolOrderResource = defaultToolOrderResource;
    }
    
    public void init() {
        // load in the tool order, if specified, from the sakai home area
        if (toolOrderFile != null)
        {
            File f = new File(toolOrderFile);
            if (f.exists())
            {
                FileInputStream fis = null;
                try
                {
                    fis = new FileInputStream(f);
                    if ( ! useToolGroup )  // default, legacy toolOrder.xml format
                        loadToolOrder(fis);
                    else                    // optional format with tool groups
                        loadToolGroups(fis);
                }
                catch (Exception t)
                {
                    log.warn("init(): trouble loading tool order from : " + toolOrderFile, t);
                }
                finally {
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            else
            {
                // start with the distributed defaults from the classpath
                try
                {
                    if ( ! useToolGroup )  // default, legacy toolOrder.xml format
                        loadToolOrder(defaultToolOrderResource.getInputStream());
                    else                    // optional format with tool groups
                        loadToolGroups(defaultToolOrderResource.getInputStream());
                }
                catch (Exception t)
                {
                    log.warn("init(): trouble loading tool order from default toolOrder.xml", t);
                }
            }
        }

    }

    /**
     * {@inheritDoc}
     */
    public List getToolGroup(String groupName) {
        if (groupName != null) {
            List groups = (List) m_toolGroups.get(groupName);
            if (groups != null) {
                log.debug("getToolGroup: " + groups.toString());
                return groups;
            }
        }
        return new Vector();
    }

    /**
     * {@inheritDoc}
     */
    public List getCategoryGroups(String category) {
        if (category != null) {
            List groups = (List) m_toolGroupCategories.get(category);
            if (groups != null) {
                log.debug("getCategoryGroups: " + groups.toString());
                return groups;
            }
        }
        return new Vector();
    }

    /**
     * {@inheritDoc}
     */
    public List getToolOrder(String category) {
        if (category != null) {
            List order = (List) m_toolOrders.get(category);
            if (order != null) {
                return order;
            }
        }

        return new Vector();
    }

    /**
     * {@inheritDoc}
     */
    public List getToolsRequired(String category) {
        if (category != null) {
            List order = (List) m_toolsRequired.get(category);
            if (order != null) {
                return order;
            }
        }

        return new Vector();
    }

    /**
     * {@inheritDoc}
     */
    public List getDefaultTools(String category) {
        if (category != null) {
            List order = (List) m_defaultTools.get(category);
            if (order != null) {
                return order;
            }
        }

        return new Vector();
    }

    /**
     * {@inheritDoc}
     */
    public List<String> getToolCategories(String category) {
        if (category != null) {
            List<String> categories = m_toolCategoriesList.get(category);
            if (categories != null) {
                return categories;
            }
        }

        return new Vector();
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, List<String>> getToolCategoriesAsMap(String category) {
        if (category != null) {
            Map<String, List<String>> categories = m_toolCategoriesMap.get(category);
            if (categories != null) {
                return categories;
            }
        }

        return new HashMap();
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, String> getToolToCategoryMap(String category) {
        if (category != null) {
            Map<String, String> categories = m_toolToToolCategoriesMap.get(category);
            if (categories != null) {
                return categories;
            }
        }

        return new HashMap();
    }/*
     * Load tools by group, from toolOrder.xml file with optional groups defined
     */

    void loadToolGroups(InputStream in) {
        Document doc = Xml.readDocumentFromStream(in);
        Element root = doc.getDocumentElement();
        if (!root.getTagName().equals("toolGroups")) {
            log.info("loadToolGroups: invalid root element (expecting \"toolGroups\"): " + root.getTagName());
            return;
        }

        NodeList groupNodes = root.getElementsByTagName("group");
        if (groupNodes != null) {
            for (int k = 0; k < groupNodes.getLength(); k++) {
                Node g_node = groupNodes.item(k);
                if (g_node.getNodeType() != Node.ELEMENT_NODE) continue;
                Element g_element = (Element) g_node;
                String groupName = StringUtils.trimToNull(g_element.getAttribute("name"));
                //
                if ((groupName != null)) {
                    // group of this name already in map?
                    List groupList = (List) m_toolGroups.get(groupName);
                    if (groupList == null) {
                        groupList = new Vector();
                        m_toolGroups.put(groupName, groupList);
                    }
                    // add tools
                    NodeList tools = g_element.getElementsByTagName("tool");
                    final int toolCount = tools.getLength();
                    for (int j = 0; j < toolCount; j++) {
                        Element toolElement = (Element) tools.item(j);
                        // add this tool
                        String toolId = toolElement.getAttribute("id");
                        groupList.add(toolId);
                        String req = StringUtils.trimToNull(toolElement.getAttribute("required"));
                        if ((req != null) && (Boolean.TRUE.toString().equalsIgnoreCase(req))) {
                            List reqList = (List) m_toolGroupRequired.get(groupName);
                            if (reqList == null) {
                                reqList = new ArrayList<String>();
                                m_toolGroupRequired.put(groupName, reqList);
                            }
                            reqList.add(toolId);
                        }
                        String sel = StringUtils.trimToNull(toolElement.getAttribute("selected"));
                        if ((sel != null) && (Boolean.TRUE.toString().equalsIgnoreCase(sel))) {
                            List selList = (List) m_toolGroupSelected.get(groupName);
                            if (selList == null) {
                                selList = new ArrayList<String>();
                                m_toolGroupSelected.put(groupName, selList);
                            }
                            selList.add(toolId);
                        }
                    }
                    // add group to category(s)
                    String groupCategories = StringUtils.trimToNull(g_element.getAttribute("category"));
                    if (groupCategories != null) {
                        List<String> list = new ArrayList<String>(Arrays.asList(groupCategories.split(",")));
                        //noinspection ForLoopReplaceableByForEach
                        for (Iterator<String> itr = list.iterator(); itr.hasNext(); ) {
                            String catName = itr.next();
                            List groupCategoryList = (List) m_toolGroupCategories.get(catName);
                            if (groupCategoryList == null) {
                                groupCategoryList = new ArrayList();
                                m_toolGroupCategories.put(catName, groupCategoryList);
                            }
                            groupCategoryList.add(groupName);
                        }
                    }
                }
            }
        }
    }/*
     * Returns true if selected tool is contained in pre-initialized list of selected items
     * @parms toolId id of the selected tool
     */

    public boolean toolGroupIsSelected(String groupName, String toolId) {
        List selList = (List) m_toolGroupRequired.get(groupName);
        if (selList == null) {
            return false;
        } else {
            int result = selList.indexOf(toolId);
            return result >= 0;
        }
    }/*
     * Returns true if selected tool is contained in pre-initialized list of required items
     * @parms toolId id of the selected tool
     */

    public boolean toolGroupIsRequired(String groupName, String toolId) {
        List reqList = (List) m_toolGroupRequired.get(groupName);
        if (reqList == null) {
            return false;
        } else {
            int result = reqList.indexOf(toolId);
            return result >= 0;
        }
    }

    /**
     * Load this single file as a registration file, loading tools and locks.
     *
     * @param in The Stream to load
     */
    void loadToolOrder(InputStream in) {
        Document doc = Xml.readDocumentFromStream(in);
        Element root = doc.getDocumentElement();
        if (!root.getTagName().equals("toolOrder")) {
            log.info("loadToolOrder: invalid root element (expecting \"toolOrder\"): " + root.getTagName());
            return;
        }

        // read the children nodes
        NodeList rootNodes = root.getChildNodes();
        final int rootNodesLength = rootNodes.getLength();
        for (int i = 0; i < rootNodesLength; i++) {
            Node rootNode = rootNodes.item(i);
            if (rootNode.getNodeType() != Node.ELEMENT_NODE) continue;
            Element rootElement = (Element) rootNode;

            // look for "category" elements
            if (rootElement.getTagName().equals("category")) {
                String name = StringUtils.trimToNull(rootElement.getAttribute("name"));
                if (name != null) {
                    // form a list for this category
                    List order = (List) m_toolOrders.get(name);
                    if (order == null) {
                        order = new Vector();
                        m_toolOrders.put(name, order);

                        List required = new Vector();
                        m_toolsRequired.put(name, required);
                        List defaultTools = new Vector();
                        m_defaultTools.put(name, defaultTools);

                        List<String> toolCategories = new Vector();
                        m_toolCategoriesList.put(name, toolCategories);

                        Map<String, List<String>> toolCategoryMappings = new HashMap();
                        m_toolCategoriesMap.put(name, toolCategoryMappings);

                        Map<String, String> toolToCategoryMap = new HashMap();
                        m_toolToToolCategoriesMap.put(name, toolToCategoryMap);

                        // get the kids
                        NodeList nodes = rootElement.getChildNodes();
                        final int nodesLength = nodes.getLength();
                        for (int c = 0; c < nodesLength; c++) {
                            Node node = nodes.item(c);
                            if (node.getNodeType() != Node.ELEMENT_NODE) continue;
                            Element element = (Element) node;

                            if (element.getTagName().equals("tool")) {
                                processTool(element, order, required, defaultTools);
                            } else if (element.getTagName().equals("toolCategory")) {
                                processCategory(element, order, required, defaultTools,
                                        toolCategories, toolCategoryMappings, toolToCategoryMap);
                            }
                        }
                    }
                }
            }
        }
    }

    void processCategory(Element element, List order, List required,
                         List defaultTools, List<String> toolCategories,
                         Map<String, List<String>> toolCategoryMappings,
                         Map<String, String> toolToCategoryMap) {
        String name = element.getAttribute("id");
        NodeList nameList = element.getElementsByTagName("name");

        if (nameList.getLength() > 0) {
            Element nameElement = (Element) nameList.item(0);
            name = nameElement.getTextContent();
        }

        toolCategories.add(name);
        List<String> toolCategoryTools = new Vector();
        toolCategoryMappings.put(name, toolCategoryTools);

        NodeList nodes = element.getChildNodes();
        final int nodesLength = nodes.getLength();
        for (int c = 0; c < nodesLength; c++) {
            Node node = nodes.item(c);
            if (node.getNodeType() != Node.ELEMENT_NODE) continue;
            Element toolElement = (Element) node;

            if (toolElement.getTagName().equals("tool")) {
                String id = processTool(toolElement, order, required, defaultTools);
                toolCategoryTools.add(id);
                toolToCategoryMap.put(id, name);
            }
        }
    }

    String processTool(Element element, List order, List required, List defaultTools) {
        String id = StringUtils.trimToNull(element.getAttribute("id"));
        if (id != null) {
            order.add(id);
        }

        String req = StringUtils.trimToNull(element.getAttribute("required"));
        if ((req != null) && (Boolean.TRUE.toString().equalsIgnoreCase(req))) {
            required.add(id);
        }

        String sel = StringUtils.trimToNull(element.getAttribute("selected"));
        if ((sel != null) && (Boolean.TRUE.toString().equalsIgnoreCase(sel))) {
            defaultTools.add(id);
        }
        return id;
    }
}