package alien4cloud.topology;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Resource;

import org.apache.commons.collections4.MapUtils;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.collect.Sets;
import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;

import alien4cloud.component.ICSARRepositoryIndexerService;
import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.component.IToscaElementFinder;
import alien4cloud.csar.services.CsarService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.components.CSARDependency;
import alien4cloud.model.components.CapabilityDefinition;
import alien4cloud.model.components.Csar;
import alien4cloud.model.components.DeploymentArtifact;
import alien4cloud.model.components.IValue;
import alien4cloud.model.components.ImplementationArtifact;
import alien4cloud.model.components.IndexedCapabilityType;
import alien4cloud.model.components.IndexedModelUtils;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.components.IndexedRelationshipType;
import alien4cloud.model.components.IndexedToscaElement;
import alien4cloud.model.components.Interface;
import alien4cloud.model.components.Operation;
import alien4cloud.model.components.OperationOutput;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.model.components.RequirementDefinition;
import alien4cloud.model.components.ScalarPropertyValue;
import alien4cloud.model.templates.TopologyTemplate;
import alien4cloud.model.templates.TopologyTemplateVersion;
import alien4cloud.model.topology.Capability;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.RelationshipTemplate;
import alien4cloud.model.topology.Requirement;
import alien4cloud.model.topology.SubstitutionTarget;
import alien4cloud.model.topology.Topology;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ToscaParsingUtil;
import alien4cloud.utils.MapUtil;
import alien4cloud.utils.PropertyUtil;

@Service
public class TopologyServiceCore {

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    @Resource
    private ICSARRepositorySearchService csarRepoSearchService;

    @Resource
    private TopologyTemplateVersionService topologyTemplateVersionService;

    @Resource
    private CsarService csarService;

    @Resource
    private ICSARRepositoryIndexerService indexerService;

    /**
     * The default tosca element finder will search into repo.
     */
    private IToscaElementFinder repoToscaElementFinder = new IToscaElementFinder() {
        @Override
        public <T extends IndexedToscaElement> T getElementInDependencies(Class<T> elementClass, String elementId, Collection<CSARDependency> dependencies) {
            return csarRepoSearchService.getElementInDependencies(elementClass, elementId, dependencies);
        }
    };

    public Topology getTopology(String topologyId) {
        return alienDAO.findById(Topology.class, topologyId);
    }

    /**
     * Retrieve a topology given its Id
     *
     * @param topologyId id of the topology
     * @return the found topology, throws NotFoundException if not found
     */
    public Topology getOrFail(String topologyId) {
        Topology topology = getTopology(topologyId);
        if (topology == null) {
            throw new NotFoundException("Topology [" + topologyId + "] cannot be found");
        }
        return topology;
    }

    /**
     * Get the Map of {@link NodeTemplate} from a topology
     *
     * @param topology the topology
     * @return this topology's node templates
     */
    public Map<String, NodeTemplate> getNodeTemplates(Topology topology) {
        Map<String, NodeTemplate> nodeTemplates = topology.getNodeTemplates();
        if (nodeTemplates == null) {
            throw new NotFoundException("Topology [" + topology.getId() + "] do not have any node template");
        }
        return nodeTemplates;
    }

    /**
     * Get a {@link NodeTemplate} given its name from a map
     *
     * @param topologyId the topology's id
     * @param nodeTemplateName the name of the node template
     * @param nodeTemplates the topology's node templates
     * @return the found node template, throws NotFoundException if not found
     */
    public NodeTemplate getNodeTemplate(String topologyId, String nodeTemplateName, Map<String, NodeTemplate> nodeTemplates) {
        NodeTemplate nodeTemplate = nodeTemplates.get(nodeTemplateName);
        if (nodeTemplate == null) {
            throw new NotFoundException("Topology [" + topologyId + "] do not have node template with name [" + nodeTemplateName + "]");
        }
        return nodeTemplate;
    }

    /**
     * Get a {@link NodeTemplate} given its name from a topology
     *
     * @param topology the topology
     * @param nodeTemplateId the name of the node template
     * @return the found node template, throws NotFoundException if not found
     */
    public NodeTemplate getNodeTemplate(Topology topology, String nodeTemplateId) {
        Map<String, NodeTemplate> nodeTemplates = getNodeTemplates(topology);
        return getNodeTemplate(topology.getId(), nodeTemplateId, nodeTemplates);
    }

    /**
     * Get the indexed node types used in a topology.
     *
     * @param topology The topology for which to get indexed node types.
     * @param abstractOnly If true, only abstract types will be retrieved.
     * @param useTemplateNameAsKey If true the name of the node template will be used as key for the type in the returned map, if not the type will be used as
     *            key.
     * @return A map of indexed node types.
     */
    public Map<String, IndexedNodeType> getIndexedNodeTypesFromTopology(Topology topology, boolean abstractOnly, boolean useTemplateNameAsKey) {
        return getIndexedNodeTypesFromDependencies(topology.getNodeTemplates(), topology.getDependencies(), abstractOnly, useTemplateNameAsKey);
    }

    public Map<String, IndexedNodeType> getIndexedNodeTypesFromDependencies(Map<String, NodeTemplate> nodeTemplates, Set<CSARDependency> dependencies,
            boolean abstractOnly, boolean useTemplateNameAsKey) {
        Map<String, IndexedNodeType> nodeTypes = Maps.newHashMap();
        if (nodeTemplates == null) {
            return nodeTypes;
        }
        for (Map.Entry<String, NodeTemplate> template : nodeTemplates.entrySet()) {
            if (!nodeTypes.containsKey(template.getValue().getType())) {
                IndexedNodeType nodeType = csarRepoSearchService.getRequiredElementInDependencies(IndexedNodeType.class, template.getValue().getType(),
                        dependencies);
                if (!abstractOnly || nodeType.isAbstract()) {
                    String key = useTemplateNameAsKey ? template.getKey() : template.getValue().getType();
                    nodeTypes.put(key, nodeType);
                }
            }
        }
        return nodeTypes;
    }

    /**
     * Get IndexedRelationshipType in a topology
     *
     * @param topology the topology to find all relationship types
     * @return the map containing rel
     */
    public Map<String, IndexedRelationshipType> getIndexedRelationshipTypesFromTopology(Topology topology) {
        Map<String, IndexedRelationshipType> relationshipTypes = Maps.newHashMap();
        if (topology.getNodeTemplates() == null) {
            return relationshipTypes;
        }
        for (Map.Entry<String, NodeTemplate> templateEntry : topology.getNodeTemplates().entrySet()) {
            NodeTemplate template = templateEntry.getValue();
            if (template.getRelationships() != null) {
                for (Map.Entry<String, RelationshipTemplate> relationshipEntry : template.getRelationships().entrySet()) {
                    RelationshipTemplate relationship = relationshipEntry.getValue();
                    if (!relationshipTypes.containsKey(relationship.getType())) {
                        IndexedRelationshipType relationshipType = csarRepoSearchService.getRequiredElementInDependencies(IndexedRelationshipType.class,
                                relationship.getType(), topology.getDependencies());
                        relationshipTypes.put(relationship.getType(), relationshipType);
                    }
                }
            }
        }
        return relationshipTypes;
    }

    /**
     * Get IndexedRelationshipType in a topology
     *
     * @param topology the topology to find all relationship types
     * @return the map containing rel
     */
    public Map<String, IndexedCapabilityType> getIndexedCapabilityTypesFromTopology(Topology topology) {
        Map<String, IndexedCapabilityType> capabilityTypes = Maps.newHashMap();
        if (topology.getNodeTemplates() == null) {
            return capabilityTypes;
        }
        for (Map.Entry<String, NodeTemplate> templateEntry : topology.getNodeTemplates().entrySet()) {
            NodeTemplate template = templateEntry.getValue();
            if (template.getCapabilities() != null) {
                for (Map.Entry<String, Capability> capabilityEntry : template.getCapabilities().entrySet()) {
                    Capability capability = capabilityEntry.getValue();
                    if (!capabilityTypes.containsKey(capability.getType())) {
                        IndexedCapabilityType capabilityType = csarRepoSearchService.getRequiredElementInDependencies(IndexedCapabilityType.class,
                                capability.getType(), topology.getDependencies());
                        capabilityTypes.put(capability.getType(), capabilityType);
                    }
                }
            }
        }
        return capabilityTypes;
    }

    public NodeTemplate buildNodeTemplate(Set<CSARDependency> dependencies, IndexedNodeType indexedNodeType, NodeTemplate templateToMerge) {
        return buildNodeTemplate(dependencies, indexedNodeType, templateToMerge, repoToscaElementFinder);
    }

    /**
     * Build a node template
     *
     * @param dependencies the dependencies on which new node will be constructed
     * @param indexedNodeType the type of the node
     * @param templateToMerge the template that can be used to merge into the new node template
     * @return new constructed node template
     */
    public static NodeTemplate buildNodeTemplate(Set<CSARDependency> dependencies, IndexedNodeType indexedNodeType, NodeTemplate templateToMerge,
            IToscaElementFinder toscaElementFinder) {
        NodeTemplate nodeTemplate = new NodeTemplate();
        if (templateToMerge==null) {
            templateToMerge = new NodeTemplate();
        }
        
        nodeTemplate.setType(indexedNodeType.getElementId());
        setFirstNonNull(nodeTemplate::setName, templateToMerge::getName);
        
        // Note some things (interface artifacts) might have deferred parsers so won't be set yet;
        // and the deferred parsers have references to instances that the merge method (below) might replace.
        // We do a deep merge of node templates at the end. Here just copy from the template to merge. 

        nodeTemplate.setArtifacts(templateToMerge.getArtifacts());
        nodeTemplate.setInterfaces(templateToMerge.getInterfaces());
        
        Map<String, Capability> capabilities = Maps.newLinkedHashMap();
        Map<String, Requirement> requirements = Maps.newLinkedHashMap();
        Map<String, AbstractPropertyValue> properties = Maps.newLinkedHashMap();
        Map<String, String> attributes = Maps.newLinkedHashMap();
        
        fillCapabilitiesMap(capabilities, indexedNodeType.getCapabilities(), dependencies, templateToMerge.getCapabilities(), toscaElementFinder);
        fillRequirementsMap(requirements, indexedNodeType.getRequirements(), dependencies, templateToMerge.getRequirements(), toscaElementFinder);
        fillProperties(properties, indexedNodeType.getProperties(), templateToMerge.getProperties());
        fillAttributes(attributes, indexedNodeType.getAttributes());
        
        nodeTemplate.setCapabilities(capabilities);
        nodeTemplate.setRequirements(requirements);
        nodeTemplate.setProperties(properties);
        nodeTemplate.setAttributes(attributes);
        
        nodeTemplate.setRelationships(templateToMerge.getRelationships());
        
        return nodeTemplate;
    }
    
    private static ThreadLocal<Csar> currentArchive = ThreadLocal.withInitial(() -> null);
    
    public static void mergeAncestorData(NodeTemplate target, ParsingContextExecution context, ICSARRepositorySearchService searchService) {
        final ArchiveRoot archiveRoot = (ArchiveRoot) context.getRoot().getWrappedInstance();
        currentArchive.set(archiveRoot.getArchive());
        Set<String> ancestorsVisited = Sets.newLinkedHashSet();
        Set<String> ancestorsToVisit = Sets.newLinkedHashSet();
        ancestorsToVisit.add(target.getType());
        while (!ancestorsToVisit.isEmpty()) {
            String ancestorName = ancestorsToVisit.iterator().next();
            ancestorsToVisit.remove(ancestorName);
            ancestorsVisited.add(ancestorName);
            IndexedNodeType ancestor = ToscaParsingUtil.getNodeTypeFromArchiveOrDependencies(target.getType(), archiveRoot, searchService);
            if (ancestor!=null) {
                mergeSelectedAncestorData(target, target, ancestor);
                for (String anotherAncestor: ancestor.getDerivedFrom()) {
                    if (!ancestorsVisited.contains(anotherAncestor)) {
                        ancestorsToVisit.add(anotherAncestor);
                    }
                }
            }
        }
        currentArchive.set(null);
    }
    
    private static void mergeSelectedAncestorData(NodeTemplate target, NodeTemplate source, IndexedNodeType ancestor) {
        Map<String, DeploymentArtifact> deploymentArtifacts = mergeMaps(source.getArtifacts(), ancestor.getArtifacts(), 
            TopologyServiceCore::mergeDeploymentArtifact);
        target.setArtifacts(deploymentArtifacts);

        setFirstNonNull(target::setGroups, source::getGroups);
        target.setInterfaces(mergeMaps(source.getInterfaces(), ancestor.getInterfaces(),
            TopologyServiceCore::mergeInterface));
    }

    @SafeVarargs
    private static <T,V> void setFirstNonNull(T target, BiConsumer<T,V> setter, Function<T,V> getter, T ...sources) {
        merge(target, setter, (v1,v2)->(v1!=null ? v1 : v2), getter, sources);
    }

    @SafeVarargs
    private static <T,V> void merge(T target, BiConsumer<T,V> setter, BiFunction<V,V,V> mergeFunction, Function<T,V> getter, T ...sources) {
        V value = getter.apply(target);
        for (T source: sources) {
            if (source!=null) {
                V v2 = getter.apply(source);
                if (value==null) {
                    value = v2;
                } else if (v2!=null) {
                    value = mergeFunction.apply(value, v2);
                }
            }
        }
        setter.accept(target, value);
    }

    // alternate format for when sources are different types
    @SafeVarargs
    private static <T,V extends Supplier<T>> void setFirstNonNull(Consumer<T> setter, V ...sources) {
        for (Supplier<T> s: sources) {
            T candidate = s.get();
            if (candidate!=null) {
                setter.accept(candidate);
                return;
            }
        }
    }
    
    @SafeVarargs
    private static <T,K,V> Map<K, V> mergeMaps(BiFunction<V,V,V> mergeFn, Function<T,Map<K,V>> getter, T ...sources) {
        LinkedHashMap<K,V> result = Maps.newLinkedHashMap();
        for (T source: sources) {
            if (source!=null && getter.apply(source)!=null) {
                getter.apply(source).forEach((k,v)->result.put(k, mergeFn.apply(result.get(k), v)));
            }
        }
        return result;
    }

    // alternate format for when sources are different types
    private static <K,V> Map<K, V> mergeMaps(Map<K, V> primary, Map<K, V> secondary, BiFunction<V,V,V> mergeFn) {
        LinkedHashMap<K,V> result = Maps.newLinkedHashMap();
        if (primary!=null) {
            primary.forEach((k,v)->result.put(k, mergeFn.apply(result.get(k), v)));
        }
        if (secondary!=null) {
            secondary.forEach((k,v)->result.put(k, mergeFn.apply(result.get(k), v)));
        }
        return result;
    }

    private static Interface mergeInterface(Interface i1, Interface i2) {
        Interface result = new Interface();
        setFirstNonNull(result, Interface::setDescription, Interface::getDescription, i1, i2);
        result.setOperations(mergeMaps(TopologyServiceCore::mergeOperation, Interface::getOperations, i1, i2));
        return result;
    }

    private static Operation mergeOperation(Operation o1, Operation o2) {
        Operation result = new Operation();
        setFirstNonNull(result, Operation::setDescription, Operation::getDescription, o1, o2);
        merge(result, Operation::setImplementationArtifact, TopologyServiceCore::mergeImplementationArtifact, Operation::getImplementationArtifact, o1, o2);
        result.setInputParameters(mergeMaps((v1,v2)->v1!=null?v1:v2, Operation::getInputParameters, o1, o2));
        result.setOutputs(mergeSetsWithKey(Operation::getOutputs, OperationOutput::getName, o1, o2));
        return result;
    }
    
    @SafeVarargs
    @SuppressWarnings("unused")
    private static <T,V> Set<V> mergeSets(Function<T,Set<V>> getter, T ...sources) {
        return mergeSetsWithKey(getter, null, sources);
    }
    @SafeVarargs
    private static <T,V,K> Set<V> mergeSetsWithKey(Function<T,Set<V>> getter, Function<V,K> optionalKey, T ...sources) {
        Set<V> result = null;
        for (T source: sources) {
            if (source!=null) result = mergeSetsWithKey(optionalKey, result, getter.apply(source));
        }
        return result;
    }

    private static <V,K> Set<V> mergeSetsWithKey(Function<V,K> optionalKey, Set<V> o1, Set<V> o2) {
        if (o1==null) return o2;
        if (o2==null) return o1;
        if (optionalKey==null) {
            Set<V> result = Sets.newLinkedHashSet();            
            result.addAll(o1);
            result.addAll(o2);
            return result;
        }
        Map<K,V> result = Maps.newLinkedHashMap();
        o2.forEach(v -> result.put(v==null? null : optionalKey.apply(v), v));
        o1.forEach(v -> result.put(v==null? null : optionalKey.apply(v), v));
        return Sets.newLinkedHashSet(result.values());
    }

    private static DeploymentArtifact mergeDeploymentArtifact(DeploymentArtifact o1, DeploymentArtifact o2) {
        DeploymentArtifact result = new DeploymentArtifact();
        
        // node archive name and version are not read from YAML - just set the outermost bundle,
        // then rely on reading to traverse all bundles
        setFirstNonNull(result, DeploymentArtifact::setArchiveName, DeploymentArtifact::getArchiveName, o1, o2);
        if (result.getArchiveName()==null && currentArchive.get()!=null) result.setArchiveName(currentArchive.get().getName());
        setFirstNonNull(result, DeploymentArtifact::setArchiveVersion, DeploymentArtifact::getArchiveVersion, o1, o2);
        if (result.getArchiveVersion()==null && currentArchive.get()!=null) result.setArchiveVersion(currentArchive.get().getVersion());
        
        setFirstNonNull(result, DeploymentArtifact::setArtifactName, DeploymentArtifact::getArtifactName, o1, o2);
        setFirstNonNull(result, DeploymentArtifact::setArtifactRef, DeploymentArtifact::getArtifactRef, o1, o2);
        setFirstNonNull(result, DeploymentArtifact::setArtifactType, DeploymentArtifact::getArtifactType, o1, o2);
        setFirstNonNull(result, DeploymentArtifact::setArtifactRepository, DeploymentArtifact::getArtifactRepository, o1, o2);
        
        return result;
    }

    private static ImplementationArtifact mergeImplementationArtifact(ImplementationArtifact o1, ImplementationArtifact o2) {
        ImplementationArtifact result = new ImplementationArtifact();
        
        setFirstNonNull(result, ImplementationArtifact::setArchiveName, ImplementationArtifact::getArchiveName, o1, o2);
        setFirstNonNull(result, ImplementationArtifact::setArchiveVersion, ImplementationArtifact::getArchiveVersion, o1, o2);
        setFirstNonNull(result, ImplementationArtifact::setArtifactRef, ImplementationArtifact::getArtifactRef, o1, o2);
        setFirstNonNull(result, ImplementationArtifact::setArtifactType, ImplementationArtifact::getArtifactType, o1, o2);
        
        // compared with DeplomentArtifact: name not available, and repository not supported (no setter)
        
        return result;

    }

    private static void fillAttributes(Map<String, String> attributes, Map<String, IValue> attributes2) {
        if (attributes2 == null || attributes == null) {
            return;
        }
        for (Entry<String, IValue> entry : attributes2.entrySet()) {
            attributes.put(entry.getKey(), null);
        }
    }

    public static void fillProperties(Map<String, AbstractPropertyValue> properties, Map<String, PropertyDefinition> propertiesDefinitions,
            Map<String, AbstractPropertyValue> map) {
        if (propertiesDefinitions == null || properties == null) {
            return;
        }
        for (Map.Entry<String, PropertyDefinition> entry : propertiesDefinitions.entrySet()) {
            AbstractPropertyValue existingValue = MapUtils.getObject(map, entry.getKey());
            if (existingValue == null) {
                String defaultValue = entry.getValue().getDefault();
                if (defaultValue != null && !defaultValue.trim().isEmpty()) {
                    properties.put(entry.getKey(), new ScalarPropertyValue(defaultValue));
                } else {
                    properties.put(entry.getKey(), null);
                }
            } else {
                properties.put(entry.getKey(), existingValue);
            }
        }
    }

    private static void fillCapabilitiesMap(Map<String, Capability> map, List<CapabilityDefinition> elements, Collection<CSARDependency> dependencies,
            Map<String, Capability> mapToMerge, IToscaElementFinder toscaElementFinder) {
        if (elements == null) {
            return;
        }
        for (CapabilityDefinition capa : elements) {
            Capability toAddCapa = MapUtils.getObject(mapToMerge, capa.getId());
            if (toAddCapa == null) {
                toAddCapa = new Capability();
                toAddCapa.setType(capa.getType());
                IndexedCapabilityType indexedCapa = toscaElementFinder.getElementInDependencies(IndexedCapabilityType.class, capa.getType(), dependencies);
                if (indexedCapa != null && indexedCapa.getProperties() != null) {
                    toAddCapa.setProperties(PropertyUtil.getDefaultPropertyValuesFromPropertyDefinitions(indexedCapa.getProperties()));
                }
            }
            map.put(capa.getId(), toAddCapa);
        }
    }

    private static void fillRequirementsMap(Map<String, Requirement> map, List<RequirementDefinition> elements, Collection<CSARDependency> dependencies,
            Map<String, Requirement> mapToMerge, IToscaElementFinder toscaElementFinder) {
        if (elements == null) {
            return;
        }
        for (RequirementDefinition requirement : elements) {
            Requirement toAddRequirement = MapUtils.getObject(mapToMerge, requirement.getId());
            if (toAddRequirement == null) {
                toAddRequirement = new Requirement();
                toAddRequirement.setType(requirement.getType());
                IndexedCapabilityType indexedReq = toscaElementFinder.getElementInDependencies(IndexedCapabilityType.class, requirement.getType(),
                        dependencies);
                if (indexedReq != null && indexedReq.getProperties() != null) {
                    toAddRequirement.setProperties(PropertyUtil.getDefaultPropertyValuesFromPropertyDefinitions(indexedReq.getProperties()));
                }
            }
            map.put(requirement.getId(), toAddRequirement);
        }
    }

    public TopologyTemplate createTopologyTemplate(Topology topology, String name, String description, String version) {
        String topologyId = UUID.randomUUID().toString();
        topology.setId(topologyId);

        String topologyTemplateId = UUID.randomUUID().toString();
        TopologyTemplate topologyTemplate = new TopologyTemplate();
        topologyTemplate.setId(topologyTemplateId);
        topologyTemplate.setName(name);
        topologyTemplate.setDescription(description);

        topology.setDelegateId(topologyTemplateId);
        topology.setDelegateType(TopologyTemplate.class.getSimpleName().toLowerCase());

        save(topology);
        this.alienDAO.save(topologyTemplate);
        if (version == null) {
            topologyTemplateVersionService.createVersion(topologyTemplateId, null, topology);
        } else {
            topologyTemplateVersionService.createVersion(topologyTemplateId, null, version, null, topology);
        }

        return topologyTemplate;

    }

    /**
     *
     * Get all the relationships in which a given node template is a target
     *
     * @param nodeTemplateName the name of the node template which is target for relationship
     * @param nodeTemplates all topology's node templates
     * @return all relationships which have nodeTemplateName as target
     */
    public List<RelationshipTemplate> getTargetRelatedRelatonshipsTemplate(String nodeTemplateName, Map<String, NodeTemplate> nodeTemplates) {
        List<RelationshipTemplate> toReturn = Lists.newArrayList();
        for (String key : nodeTemplates.keySet()) {
            NodeTemplate nodeTemp = nodeTemplates.get(key);
            if (nodeTemp.getRelationships() == null) {
                continue;
            }
            for (String key2 : nodeTemp.getRelationships().keySet()) {
                RelationshipTemplate relTemp = nodeTemp.getRelationships().get(key2);
                if (relTemp == null) {
                    continue;
                }
                if (relTemp.getTarget() != null && relTemp.getTarget().equals(nodeTemplateName)) {
                    toReturn.add(relTemp);
                }
            }
        }

        return toReturn;
    }

    public TopologyTemplate searchTopologyTemplateByName(String name) {
        Map<String, String[]> filters = MapUtil.newHashMap(new String[] { "name" }, new String[][] { new String[] { name } });
        GetMultipleDataResult<TopologyTemplate> result = alienDAO.find(TopologyTemplate.class, filters, Integer.MAX_VALUE);
        if (result.getTotalResults() > 0) {
            return result.getData()[0];
        }
        return null;
    }

    /**
     * Assign an id to the topology, save it and return the generated id.
     * 
     * @param topology
     * @return
     */
    public String saveTopology(Topology topology) {
        String topologyId = UUID.randomUUID().toString();
        topology.setId(topologyId);
        save(topology);
        return topologyId;
    }

    public void save(Topology topology) {
        topology.setLastUpdateDate(new Date());
        this.alienDAO.save(topology);
    }

    public void updateSubstitutionType(final Topology topology) {
        if (!topology.getDelegateType().equalsIgnoreCase(TopologyTemplate.class.getSimpleName())) {
            return;
        }
        if (topology.getSubstitutionMapping() == null || topology.getSubstitutionMapping().getSubstitutionType() == null) {
            return;
        }
        IndexedNodeType nodeType = csarRepoSearchService.getElementInDependencies(IndexedNodeType.class,
                topology.getSubstitutionMapping().getSubstitutionType().getElementId(), topology.getDependencies());

        TopologyTemplate topologyTemplate = alienDAO.findById(TopologyTemplate.class, topology.getDelegateId());
        TopologyTemplateVersion topologyTemplateVersion = topologyTemplateVersionService.getByTopologyId(topology.getId());

        Set<CSARDependency> inheritanceDependencies = Sets.newHashSet();
        inheritanceDependencies.add(new CSARDependency(nodeType.getArchiveName(), nodeType.getArchiveVersion()));

        // we have to search for the eventually existing CSar to update it' deps
        // actually, the csar is not renamed when the topology template is renamed (this is not quite simple to rename a csar if it
        // is used in topologies ....). So we have to search the csar using the topology id.
        Csar csar = csarService.getTopologySubstitutionCsar(topology.getId());
        if (csar == null) {
            // the csar can not be found, we create it
            String archiveName = topologyTemplate.getName();
            String archiveVersion = topologyTemplateVersion.getVersion();
            csar = new Csar(archiveName, archiveVersion);
            csar.setSubstitutionTopologyId(topology.getId());
        }
        csar.setDependencies(inheritanceDependencies);
        csar.getDependencies().addAll(topology.getDependencies());
        csarService.save(csar);

        IndexedNodeType topologyTemplateType = new IndexedNodeType();
        topologyTemplateType.setArchiveName(csar.getName());
        topologyTemplateType.setArchiveVersion(csar.getVersion());
        topologyTemplateType.setElementId(csar.getName());
        topologyTemplateType.setDerivedFrom(Lists.newArrayList(nodeType.getElementId()));
        topologyTemplateType.setSubstitutionTopologyId(topology.getId());
        List<CapabilityDefinition> capabilities = Lists.newArrayList();
        topologyTemplateType.setCapabilities(capabilities);
        List<RequirementDefinition> requirements = Lists.newArrayList();
        topologyTemplateType.setRequirements(requirements);
        // inputs from topology become properties of type
        topologyTemplateType.setProperties(topology.getInputs());
        // output attributes become attributes for the type
        Map<String, IValue> attributes = Maps.newHashMap();
        topologyTemplateType.setAttributes(attributes);
        Map<String, Set<String>> outputAttributes = topology.getOutputAttributes();
        if (outputAttributes != null) {
            for (Entry<String, Set<String>> oae : outputAttributes.entrySet()) {
                String nodeName = oae.getKey();
                NodeTemplate nodeTemplate = topology.getNodeTemplates().get(nodeName);
                IndexedNodeType nodeTemplateType = csarRepoSearchService.getRequiredElementInDependencies(IndexedNodeType.class, nodeTemplate.getType(),
                        topology.getDependencies());
                for (String attributeName : oae.getValue()) {
                    IValue ivalue = nodeTemplateType.getAttributes().get(attributeName);
                    // we have an issue here : if several nodes have the same attribute name, there is a conflict
                    if (ivalue != null && !attributes.containsKey(attributeName)) {
                        attributes.put(attributeName, ivalue);
                    }
                }
            }
        }
        // output properties become attributes for the type
        Map<String, Set<String>> outputProperties = topology.getOutputProperties();
        if (outputProperties != null) {
            for (Entry<String, Set<String>> ope : outputProperties.entrySet()) {
                String nodeName = ope.getKey();
                NodeTemplate nodeTemplate = topology.getNodeTemplates().get(nodeName);
                IndexedNodeType nodeTemplateType = csarRepoSearchService.getRequiredElementInDependencies(IndexedNodeType.class, nodeTemplate.getType(),
                        topology.getDependencies());
                for (String propertyName : ope.getValue()) {
                    PropertyDefinition pd = nodeTemplateType.getProperties().get(propertyName);
                    // we have an issue here : if several nodes have the same attribute name, there is a conflict
                    if (pd != null && !attributes.containsKey(propertyName)) {
                        attributes.put(propertyName, pd);
                    }
                }
            }
        }
        // output capabilities properties also become attributes for the type
        Map<String, Map<String, Set<String>>> outputCapabilityProperties = topology.getOutputCapabilityProperties();
        if (outputCapabilityProperties != null) {
            for (Entry<String, Map<String, Set<String>>> ocpe : outputCapabilityProperties.entrySet()) {
                String nodeName = ocpe.getKey();
                NodeTemplate nodeTemplate = topology.getNodeTemplates().get(nodeName);
                for (Entry<String, Set<String>> cpe : ocpe.getValue().entrySet()) {
                    String capabilityName = cpe.getKey();
                    String capabilityTypeName = nodeTemplate.getCapabilities().get(capabilityName).getType();
                    IndexedCapabilityType capabilityType = csarRepoSearchService.getRequiredElementInDependencies(IndexedCapabilityType.class,
                            capabilityTypeName, topology.getDependencies());
                    for (String propertyName : cpe.getValue()) {
                        PropertyDefinition pd = capabilityType.getProperties().get(propertyName);
                        // we have an issue here : if several nodes have the same attribute name, there is a conflict
                        if (pd != null && !attributes.containsKey(propertyName)) {
                            attributes.put(propertyName, pd);
                        }
                    }
                }
            }
        }

        // capabilities substitution
        if (topology.getSubstitutionMapping().getCapabilities() != null) {
            for (Entry<String, SubstitutionTarget> e : topology.getSubstitutionMapping().getCapabilities().entrySet()) {
                String key = e.getKey();
                String nodeName = e.getValue().getNodeTemplateName();
                String capabilityName = e.getValue().getTargetId();
                NodeTemplate nodeTemplate = topology.getNodeTemplates().get(nodeName);
                IndexedNodeType nodeTemplateType = csarRepoSearchService.getRequiredElementInDependencies(IndexedNodeType.class, nodeTemplate.getType(),
                        topology.getDependencies());
                CapabilityDefinition capabilityDefinition = IndexedModelUtils.getCapabilityDefinitionById(nodeTemplateType.getCapabilities(), capabilityName);
                capabilityDefinition.setId(key);
                topologyTemplateType.getCapabilities().add(capabilityDefinition);
            }
        }
        // requirement substitution
        if (topology.getSubstitutionMapping().getRequirements() != null) {
            for (Entry<String, SubstitutionTarget> e : topology.getSubstitutionMapping().getRequirements().entrySet()) {
                String key = e.getKey();
                String nodeName = e.getValue().getNodeTemplateName();
                String requirementName = e.getValue().getTargetId();
                NodeTemplate nodeTemplate = topology.getNodeTemplates().get(nodeName);
                IndexedNodeType nodeTemplateType = csarRepoSearchService.getRequiredElementInDependencies(IndexedNodeType.class, nodeTemplate.getType(),
                        topology.getDependencies());
                RequirementDefinition requirementDefinition = IndexedModelUtils.getRequirementDefinitionById(nodeTemplateType.getRequirements(),
                        requirementName);
                requirementDefinition.setId(key);
                topologyTemplateType.getRequirements().add(requirementDefinition);
            }
        }
        indexerService.indexInheritableElement(csar.getName(), csar.getVersion(), topologyTemplateType, inheritanceDependencies);
    }

}
