Athena-Dolly는 Infinispan Data Grid를 이용한 WAS에 비종속적인 세션 클러스터링 솔루션으로 현재 Apache Tomcat 6/7, JBoss EAP 6이 지원 가능하며
추후 Weblogic, Jeus, WebSphere등 다양한 WAS로도 지원을 확대할 계획이다.

1. Athena-Dolly의 설치
   - mvn install을 수행하게 되면 target 디렉토리에 playce-dolly-1.1.0-bin.zip 파일이 생성되며,
     해당 파일을 설치하고자 하는 서버로 복사한다.
   - playce-dolly-1.1.0-bin.zip 파일을 압축 해제하게 되면 dolly-agent 라는 디렉토리가 생성되고,
     하위에 dolly.properties 파일 및 lib 디렉토리가 존재하며 lib 디렉토리 안에 관련 라이브러리 파일들이 존재한다.
   - dolly.properties 파일에는 BCI를 위한 타깃 클래스 목록 및 Infinispan 관련 설정이 포함되며, 
     infinispan.client.hotrod.server_list 항목은 ";" 구분자를 이용하여 현재 동작하고 있는 Infinispan hotrod
     서버 목록으로 수정해야 한다.

2. Athena-Dolly 실행을 위한 WAS 구동 옵션 추가
   - Athena-Dolly 실행을 위해서 dolly.properties에 해당하는 System Property 및 javaagent 옵션이 필요하다.
   (eg) -Ddolly.properties=/opt/dolly-agent/dolly.properties 
        -javaagent:/opt/dolly-agent/lib/playce-dolly-1.1.0.jar

3. 상태정보 확인
   - WAS 구동 후 http://${SERVER_IP}:${SERVER_PORT}/${CONTEXT}/dolly_stats.jsp 파일을 호출하면
     Infinispan Properties, Infinispan Stats, Cache Data List 조회 화면이 표시된다.
     단, WAS에 따라서 표시가 되지 않을 수 있으며 playce-dolly-1.1.0.jar 파일 내의
     /META-INF/resources/dolly_stats.jsp 파일을 WebRoot로 복사하면 호출 가능하다.
     
4. Infinispan file-store 활성화
   - Infinispan 서버에 Eviction과 Expiration 관련 옵션이 주어지지 않을 경우 데이터가 무한 적재되면서 OutOfMemory가 발생할 가능성이 있기 때문에
     다음과 같이 캐시에 eviction 설정을 추가하고 evict 된 데이터를 파일로 저장할 수 있도록 file-store 설정을 추가한다.
     
     <distributed-cache name="default" mode="SYNC" segments="20" owners="2" remote-timeout="30000" start="EAGER">
        <locking isolation="READ_COMMITTED" acquire-timeout="30000" concurrency-level="1000" striping="false"/>
        <transaction mode="NONE"/>
        <eviction strategy="LRU" max-entries="8192"/>
        <!-- expiration은 dolly.properties 파일의 dolly.session.timeout 값으로 대체하여 설정된다.(lifespan은 -1, max-idle은 ${dolly.session.timeout}) --> 
        <file-store passivation="true" path="dolly" purge="false" preload="true" shared="true" />
    </distributed-cache>
    
    file-store에 포함될 수 있는 Attributes는 다음과 같다.
    
    a. max-entries :Sets the maximum number of in-memory mappings between keys and their position in the store.
                    Normally this is unlimited, but to avoid excess memory usage, an upper bound can be configured.
                    If this limit is exceeded, entries are removed permanently using the LRU algorithm both from
                    the in-memory index and the underlying file based cache store. Warning: setting this value
                    may cause data loss.
    b. relative-to : The base directory in which to store the cache state.
    c. path : The path within "relative-to" in which to store the cache state. If undefined, the path defaults to the cache container name.
    d. write-behind : Configures a cache store as write-behind instead of write-through.
    e. property : A cache store property with name and value.
    f. name : Uniquely identifies this store.
    g. shared : This setting should be set to true when multiple cache instances share the same cache store (e.g., multiple nodes in a cluster using a JDBC-based CacheStore pointing to the same, shared database.) Setting this to true avoids multiple cache instances writing the same modification multiple times. If enabled, only the node where the modification originated will write to the cache store. If disabled, each individual cache reacts to a potential remote update by storing the data to the cache store.
    h. preload : If true, when the cache starts, data stored in the cache store will be pre-loaded into memory. This is particularly useful when data in the cache store will be needed immediately after startup and you want to avoid cache operations being delayed as a result of loading this data lazily. Can be used to provide a 'warm-cache' on startup, however there is a performance penalty as startup time is affected by this process.
    i. passivation : If true, data is only written to the cache store when it is evicted from memory, a phenomenon known as 'passivation'. Next time the data is requested, it will be 'activated' which means that data will be brought back to memory and removed from the persistent store. If false, the cache store contains a copy of the contents in memory, so writes to cache result in cache store writes. This essentially gives you a 'write-through' configuration.
    j. fetch-state : If true, fetch persistent state when joining a cluster. If multiple cache stores are chained, only one of them can have this property enabled.
    k. purge : If true, purges this cache store when it starts up.
    l. singleton : If true, the singleton store cache store is enabled. SingletonStore is a delegating cache store used for situations when only one instance in a cluster should interact with the underlying store.
    m. read-only : If true, the cache store will only be used to load entries. Any modifications made to the caches will not be applied to the store.
     
+:+:+:+: WAS 별 실행 결과 +:+:+:+:

1. Apache Tomcat 6 / 7
   - 이상없음

2. JBoss EAP 5
   -    [Dolly] Dolly agent activated.
		[Dolly] Target classes :
		   - org/apache/catalina/session/StandardSessionFacade
		   - org/apache/catalina/session/ManagerBase
		Failed to boot JBoss:
		java.lang.IllegalStateException: Incompletely deployed:
		
		DEPLOYMENTS IN ERROR:
		  Deployment "propertyeditors-classloader:0.0.0$MODULE" is in error due to: java.lang.NoClassDefFoundError: Could not initialize class org.jboss.aop.AspectManager
		  Deployment "aop-classloader:0.0.0" is in error due to: java.lang.NoClassDefFoundError: Could not initialize class org.jboss.aop.AspectManager
		  Deployment "deployers-classloader:0.0.0$MODULE" is in error due to: java.lang.NoClassDefFoundError: Could not initialize class org.jboss.aop.AspectManager
		  Deployment "profile-classloader:0.0.0$MODULE" is in error due to: java.lang.NoClassDefFoundError: Could not initialize class org.jboss.aop.AspectManager
		  Deployment "bootstrap-classloader:0.0.0" is in error due to: java.lang.NoClassDefFoundError: Could not initialize class org.jboss.aop.AspectManager
		  Deployment "ClassLoading" is in error due to: java.lang.NoClassDefFoundError: Could not initialize class org.jboss.aop.AspectManager
		  Deployment "profile-classloader:0.0.0" is in error due to: java.lang.NoClassDefFoundError: Could not initialize class org.jboss.aop.AspectManager
		  Deployment "JBossServer" is in error due to: java.lang.NoSuchMethodError: org.jboss.logging.Logger.getLoggerPlugin()Lorg/jboss/logging/LoggerPlugin;
		  Deployment "security-classloader:0.0.0$MODULE" is in error due to: java.lang.NoClassDefFoundError: Could not initialize class org.jboss.aop.AspectManager
		  Deployment "bootstrap-classloader:0.0.0$MODULE" is in error due to: java.lang.NoClassDefFoundError: Could not initialize class org.jboss.aop.AspectManager
		  Deployment "propertyeditors-classloader:0.0.0" is in error due to: java.lang.NoClassDefFoundError: Could not initialize class org.jboss.aop.AspectManager
		  Deployment "security-classloader:0.0.0" is in error due to: java.lang.NoClassDefFoundError: Could not initialize class org.jboss.aop.AspectManager
		  Deployment "ClassLoaderSystem" is in error due to: java.lang.NoClassDefFoundError: Could not initialize class org.jboss.aop.AspectManager
		  Deployment "jmx-classloader:0.0.0$MODULE" is in error due to: java.lang.NoClassDefFoundError: Could not initialize class org.jboss.aop.AspectManager
		  Deployment "VFSCache$realCache#1" is in error due to: java.lang.NoClassDefFoundError: Could not initialize class org.jboss.aop.AspectManager
		  Deployment "jmx-classloader:0.0.0" is in error due to: java.lang.NoClassDefFoundError: Could not initialize class org.jboss.aop.AspectManager
		  Deployment "deployers-classloader:0.0.0" is in error due to: java.lang.NoClassDefFoundError: Could not initialize class org.jboss.aop.AspectManager
		  Deployment "LogBridgeHandler" is in error due to: java.lang.NoClassDefFoundError: Could not initialize class org.jboss.aop.AspectManager
		  Deployment "VfsNamesExceptionHandler" is in error due to: java.lang.NoClassDefFoundError: Could not initialize class org.jboss.aop.AspectManager
		  Deployment "aop-classloader:0.0.0$MODULE" is in error due to: java.lang.NoClassDefFoundError: Could not initialize class org.jboss.aop.AspectManager
		
		DEPLOYMENTS MISSING DEPENDENCIES:
		  Deployment "ManagedObjectCreator" is missing the following dependencies:
		    Dependency "ManagedObjectFactory" (should be in state "Installed", but is actually in state "Not Installed")
		    Dependency "deployers-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "ClassLoadingMetaDataParser" is missing the following dependencies:
		    Dependency "deployers-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "ProfileFactory" is missing the following dependencies:
		    Dependency "profile-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "PersistenceFactory" is missing the following dependencies:
		    Dependency "profile-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		    Dependency "ManagedObjectFactory" (should be in state "Installed", but is actually in state "Not Installed")
		  Deployment "AOPAnnotationMetaDataParserDeployer" is missing the following dependencies:
		    Dependency "AOPXMLMetaDataParserDeployer" (should be in state "Installed", but is actually in state "Not Installed")
		    Dependency "deployers-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "StructureModificationChecker" is missing the following dependencies:
		    Dependency "WebVisitorAttributes" (should be in state "Installed", but is actually in state "Not Installed")
		    Dependency "WebVisitorAttributes" (should be in state "Installed", but is actually in state "Not Installed")
		    Dependency "SynchAdapter" (should be in state "Installed", but is actually in state "Not Installed")
		    Dependency "MetaDataStructureModificationChecker" (should be in state "Installed", but is actually in state "Not Installed")
		    Dependency "profile-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "StructureBuilder" is missing the following dependencies:
		    Dependency "ModificationTypeStructureProcessor" (should be in state "Installed", but is actually in state "Not Installed")
		    Dependency "deployers-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "BeanMetaDataICF" is missing the following dependencies:
		    Dependency "jboss.kernel:service=KernelController" (should be in state "Installed", but is actually in state "**ERROR**")
		    Dependency "deployers-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		    Dependency "ManagedObjectFactory" (should be in state "Installed", but is actually in state "Not Installed")
		  Deployment "ServiceMetaDataICF" is missing the following dependencies:
		    Dependency "JMXKernel" (should be in state "Installed", but is actually in state "Not Installed")
		    Dependency "JMXKernel" (should be in state "Installed", but is actually in state "Not Installed")
		    Dependency "deployers-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "ProfileServicePersistenceDeployer" is missing the following dependencies:
		    Dependency "profile-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		    Dependency "PersistenceFactory" (should be in state "Installed", but is actually in state "Not Installed")
		    Dependency "AttachmentStore" (should be in state "Installed", but is actually in state "Not Installed")
		  Deployment "AOPClassLoaderScopingPolicy" is missing the following dependencies:
		    Dependency "aop-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "StructureModCache" is missing the following dependencies:
		    Dependency "profile-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "ProfileServiceBootstrap" is missing the following dependencies:
		    Dependency "BootstrapProfileFactory" (should be in state "Installed", but is actually in state "Not Installed")
		    Dependency "profile-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		    Dependency "ManagedObjectFactory" (should be in state "Installed", but is actually in state "Not Installed")
		    Dependency "ManagedDeploymentCreator" (should be in state "Installed", but is actually in state "Not Installed")
		    Dependency "MainDeployer" (should be in state "Installed", but is actually in state "Not Installed")
		    Dependency "DefaultProfileKey" (should be in state "Installed", but is actually in state "Not Installed")
		    Dependency "ProfileService" (should be in state "Installed", but is actually in state "Not Installed")
		    Dependency "jboss.kernel:service=Kernel" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "WebVisitorAttributes" is missing the following dependencies:
		    Dependency "profile-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "ManagedObjectFactory" is missing the following dependencies:
		    Dependency "MetaValueFactory" (should be in state "Installed", but is actually in state "Not Installed")
		    Dependency "deployers-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "ServiceDeploymentDeployer" is missing the following dependencies:
		    Dependency "deployers-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "AOPXMLMetaDataParserDeployer" is missing the following dependencies:
		    Dependency "deployers-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "MainDeployer" is missing the following dependencies:
		    Dependency "ManagedDeploymentCreator" (should be in state "Installed", but is actually in state "Not Installed")
		    Dependency "StructuralDeployers" (should be in state "Installed", but is actually in state "Not Installed")
		    Dependency "Deployers" (should be in state "Installed", but is actually in state "Not Installed")
		    Dependency "topContextComparator" (should be in state "Installed", but is actually in state "Not Installed")
		    Dependency "deployers-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "JARStructureCandidates" is missing the following dependencies:
		    Dependency "JARFilter" (should be in state "Installed", but is actually in state "Not Installed")
		    Dependency "deployers-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "JMXCreateDestroyAdvice" is missing the following dependencies:
		    Dependency "jmx-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		    Dependency "JMXKernel" (should be in state "Installed", but is actually in state "Not Installed")
		  Deployment "ProfileRepositoryFactory" is missing the following dependencies:
		    Dependency "profile-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "topContextComparator" is missing the following dependencies:
		    Dependency "deployers-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "BeansDeploymentAopMetaDataDeployer" is missing the following dependencies:
		    Dependency "AspectManager" (should be in state "Installed", but is actually in state "Not Installed")
		    Dependency "deployers-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		    Dependency "jboss.kernel:service=Kernel" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "KernelDeploymentComponentMapper" is missing the following dependencies:
		    Dependency "profile-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		    Dependency "PersistenceFactory" (should be in state "Installed", but is actually in state "Not Installed")
		    Dependency "ComponentMapperRegistry" (should be in state "Installed", but is actually in state "Not Installed")
		    Dependency "ComponentMapperRegistry" (should be in state "Installed", but is actually in state "Not Installed")
		  Deployment "AttachmentStore" is missing the following dependencies:
		    Dependency "profile-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		    Dependency "MainDeployer" (should be in state "Installed", but is actually in state "Not Installed")
		    Dependency "BootstrapProfileFactory" (should be in state "Installed", but is actually in state "Not Installed")
		    Dependency "PersistenceFactory" (should be in state "Installed", but is actually in state "Not Installed")
		    Dependency "AttachmentsSerializer" (should be in state "Installed", but is actually in state "Not Installed")
		  Deployment "PropertyEditorManager" is missing the following dependencies:
		    Dependency "propertyeditors-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "ServiceClassLoaderDeployer" is missing the following dependencies:
		    Dependency "deployers-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "SynchAdapter" is missing the following dependencies:
		    Dependency "profile-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "ClassLoadingDefaultDeployer" is missing the following dependencies:
		    Dependency "deployers-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "DeploymentFilter" is missing the following dependencies:
		    Dependency "profile-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "ModificationCheckerFilter" is missing the following dependencies:
		    Dependency "profile-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "JMXCreateDestroyAdvice$AspectBinding" is missing the following dependencies:
		    Dependency "AspectManager" (should be in state "Installed", but is actually in state "Not Installed")
		    Dependency "jmx-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "ProfileServiceDeployer" is missing the following dependencies:
		    Dependency "profile-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		    Dependency "AttachmentStore" (should be in state "Installed", but is actually in state "Not Installed")
		    Dependency "MainDeployer" (should be in state "Installed", but is actually in state "Not Installed")
		  Deployment "JMXRegistrationAdvice$AspectBinding" is missing the following dependencies:
		    Dependency "jmx-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		    Dependency "AspectManager" (should be in state "Installed", but is actually in state "Not Installed")
		  Deployment "BeanMetaDataDeployer" is missing the following dependencies:
		    Dependency "jboss.kernel:service=Kernel" (should be in state "Installed", but is actually in state "**ERROR**")
		    Dependency "deployers-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "KernelDeploymentDeployer" is missing the following dependencies:
		    Dependency "ManagedDeploymentCreator" (should be in state "Installed", but is actually in state "Not Installed")
		    Dependency "deployers-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		    Dependency "ManagedDeploymentCreator" (should be in state "Installed", but is actually in state "Not Installed")
		    Dependency "KernelDeploymentManagedObjectCreator" (should be in state "Installed", but is actually in state "Not Installed")
		  Deployment "JARFilter" is missing the following dependencies:
		    Dependency "deployers-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "AOPDeploymentAopMetaDataDeployer" is missing the following dependencies:
		    Dependency "jboss.kernel:service=Kernel" (should be in state "Installed", but is actually in state "**ERROR**")
		    Dependency "AspectManager" (should be in state "Installed", but is actually in state "Not Installed")
		    Dependency "deployers-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "FilteredProfileFactory" is missing the following dependencies:
		    Dependency "profile-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "ProfileService" is missing the following dependencies:
		    Dependency "profile-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		    Dependency "DefaultProfileKey" (should be in state "Installed", but is actually in state "Not Installed")
		    Dependency "ProfileServiceDeployer" (should be in state "Installed", but is actually in state "Not Installed")
		    Dependency "jboss.kernel:service=KernelController" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "ModificationTypeStructureProcessor" is missing the following dependencies:
		    Dependency "deployers-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "DefaultAspectManager" is missing the following dependencies:
		    Dependency "aop-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "DeclaredStructure" is missing the following dependencies:
		    Dependency "deployers-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "JMXStartStopAdvice$AspectBinding" is missing the following dependencies:
		    Dependency "AspectManager" (should be in state "Installed", but is actually in state "Not Installed")
		    Dependency "jmx-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "JMXStartStopAdvice" is missing the following dependencies:
		    Dependency "JMXKernel" (should be in state "Installed", but is actually in state "Not Installed")
		    Dependency "jmx-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "ServiceDeployer" is missing the following dependencies:
		    Dependency "deployers-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		    Dependency "JMXKernel" (should be in state "Installed", but is actually in state "Not Installed")
		  Deployment "VFSCache" is missing the following dependencies:
		    Dependency "VfsNamesExceptionHandler" (should be in state "Installed", but is actually in state "**ERROR**")
		    Dependency "VfsNamesExceptionHandler" (should be in state "Installed", but is actually in state "**ERROR**")
		    Dependency "VfsNamesExceptionHandler" (should be in state "Installed", but is actually in state "**ERROR**")
		    Dependency "VfsNamesExceptionHandler" (should be in state "Installed", but is actually in state "**ERROR**")
		    Dependency "VFSCache$realCache#1" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "MetaDataStructureModificationChecker" is missing the following dependencies:
		    Dependency "profile-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		    Dependency "MainDeployer" (should be in state "Installed", but is actually in state "Not Installed")
		    Dependency "StructureModCache" (should be in state "Installed", but is actually in state "Not Installed")
		    Dependency "ModificationCheckerFilter" (should be in state "Installed", but is actually in state "Not Installed")
		    Dependency "ModificationCheckerFilter" (should be in state "Installed", but is actually in state "Not Installed")
		  Deployment "AOPClassLoaderDeployer" is missing the following dependencies:
		    Dependency "AspectManager" (should be in state "Installed", but is actually in state "Not Installed")
		    Dependency "deployers-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "AspectManager" is missing the following dependencies:
		    Dependency "aop-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		    Dependency "AOPJBossIntegration" (should be in state "Installed", but is actually in state "Not Installed")
		  Deployment "BeanDeployer" is missing the following dependencies:
		    Dependency "deployers-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "AOPClassPoolFactory" is missing the following dependencies:
		    Dependency "aop-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		    Dependency "AOPClassLoaderScopingPolicy" (should be in state "Installed", but is actually in state "Not Installed")
		    Dependency "AOPRegisterModuleCallback" (should be in state "Installed", but is actually in state "Not Installed")
		  Deployment "DefaultDeploymentRepositoryFactory" is missing the following dependencies:
		    Dependency "DeploymentFilter" (should be in state "Installed", but is actually in state "Not Installed")
		    Dependency "profile-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		    Dependency "StructureModificationChecker" (should be in state "Installed", but is actually in state "Not Installed")
		  Deployment "JNDIBasedSecurityManagement" is missing the following dependencies:
		    Dependency "security-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "AttachmentsSerializer" is missing the following dependencies:
		    Dependency "profile-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		    Dependency "BootstrapProfileFactory" (should be in state "Installed", but is actually in state "Not Installed")
		  Deployment "StructuralDeployers" is missing the following dependencies:
		    Dependency "StructureBuilder" (should be in state "Installed", but is actually in state "Not Installed")
		    Dependency "deployers-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "JMXKernel" is missing the following dependencies:
		    Dependency "jboss.kernel:service=Kernel" (should be in state "Installed", but is actually in state "**ERROR**")
		    Dependency "JBossServer" (should be in state "Installed", but is actually in state "**ERROR**")
		    Dependency "ServerInfo" (should be in state "Installed", but is actually in state "Not Installed")
		    Dependency "jmx-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "ClassLoaderDescribeDeployer" is missing the following dependencies:
		    Dependency "ClassLoading" (should be in state "Installed", but is actually in state "**ERROR**")
		    Dependency "deployers-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "JMXRegistrationAdvice" is missing the following dependencies:
		    Dependency "JMXKernel" (should be in state "Installed", but is actually in state "Not Installed")
		    Dependency "jmx-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "CoreBeanAnnotationAdapter" is missing the following dependencies:
		    Dependency "CoreBeanAnnotationAdapterFactory" (should be in state "Installed", but is actually in state "Not Installed")
		    Dependency "deployers-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "AspectManagerJMXRegistrar" is missing the following dependencies:
		    Dependency "JMXKernel" (should be in state "Installed", but is actually in state "Not Installed")
		    Dependency "AspectManager" (should be in state "Installed", but is actually in state "Not Installed")
		    Dependency "jmx-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "BootstrapProfileFactory" is missing the following dependencies:
		    Dependency "profile-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		    Dependency "ProfileFactory" (should be in state "Installed", but is actually in state "Not Installed")
		  Deployment "MetaValueFactory" is missing the following dependencies:
		    Dependency "deployers-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "ServerInfo" is missing the following dependencies:
		    Dependency "jmx-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "KernelDeploymentManagedObjectCreator" is missing the following dependencies:
		    Dependency "ManagedObjectFactory" (should be in state "Installed", but is actually in state "Not Installed")
		    Dependency "deployers-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "ManagedDeploymentCreator" is missing the following dependencies:
		    Dependency "deployers-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "ClassLoaderDeployer" is missing the following dependencies:
		    Dependency "ClassLoaderSystem" (should be in state "Installed", but is actually in state "**ERROR**")
		    Dependency "deployers-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		    Dependency "ClassLoading" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "DefaultProfileKey" is missing the following dependencies:
		    Dependency "profile-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "CoreBeanAnnotationAdapterFactory" is missing the following dependencies:
		    Dependency "deployers-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "ClassLoaderClassPathDeployer" is missing the following dependencies:
		    Dependency "deployers-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "ComponentMapperRegistry" is missing the following dependencies:
		    Dependency "profile-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "InMemoryClassesDeployer" is missing the following dependencies:
		    Dependency "deployers-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "FileStructure" is missing the following dependencies:
		    Dependency "deployers-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "BasicProfileFactory" is missing the following dependencies:
		    Dependency "profile-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		    Dependency "ProfileRepositoryFactory" (should be in state "Installed", but is actually in state "Not Installed")
		  Deployment "Deployers" is missing the following dependencies:
		    Dependency "ManagedObjectCreator" (should be in state "Installed", but is actually in state "Not Installed")
		    Dependency "deployers-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		    Dependency "jboss.kernel:service=KernelController" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "JARStructure" is missing the following dependencies:
		    Dependency "JARStructureCandidates" (should be in state "Installed", but is actually in state "Not Installed")
		    Dependency "deployers-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "SARDeployer" is missing the following dependencies:
		    Dependency "deployers-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "SecurityConstantsBridge" is missing the following dependencies:
		    Dependency "JNDIBasedSecurityManagement" (should be in state "Installed", but is actually in state "Not Installed")
		    Dependency "security-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "ServiceDeploymentComponentMapper" is missing the following dependencies:
		    Dependency "PersistenceFactory" (should be in state "Installed", but is actually in state "Not Installed")
		    Dependency "profile-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		  Deployment "AOPJBossIntegration" is missing the following dependencies:
		    Dependency "aop-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		    Dependency "AOPClassPoolFactory" (should be in state "Installed", but is actually in state "Not Installed")
		    Dependency "AOPClassLoaderScopingPolicy" (should be in state "Installed", but is actually in state "Not Installed")
		  Deployment "AOPRegisterModuleCallback" is missing the following dependencies:
		    Dependency "aop-classloader:0.0.0" (should be in state "Installed", but is actually in state "**ERROR**")
		
		        at org.jboss.kernel.plugins.deployment.AbstractKernelDeployer.internalValidate(AbstractKernelDeployer.java:278)
		        at org.jboss.kernel.plugins.deployment.AbstractKernelDeployer.validate(AbstractKernelDeployer.java:174)
		        at org.jboss.bootstrap.microcontainer.ServerImpl.doStart(ServerImpl.java:142)
		        at org.jboss.bootstrap.AbstractServerImpl.start(AbstractServerImpl.java:450)
		        at org.jboss.Main.boot(Main.java:229)
		        at org.jboss.Main$1.call(Main.java:561)
		        at org.jboss.Main$1.call(Main.java:557)
		        at java.util.concurrent.FutureTask.run(FutureTask.java:262)
		        at java.lang.Thread.run(Thread.java:744)
		        
3. JBoss EAP 6
   - -Ddolly.properties, -javaagent: 옵션만 추가 시 javax.servlet.ServletException: java.lang.NoClassDefFoundError: com/athena/dolly/stats/DollyStats 또는 com/athena/dolly/stats/DollyManager 발생
   - jboss.modules.system.pkgs 옵션에 com.athena.dolly 추가 (eg. -Djboss.modules.system.pkgs=org.jboss.byteman,com.athena.dolly)
   
4. Weblogic 11R1
   - -Ddolly.properties, -javaagent: 옵션 외 -Xbootclasspath/a:/opt/dolly-agent/lib/commons-pool-1.6.jar 추가