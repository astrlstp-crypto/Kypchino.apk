#define _GNU_SOURCE
#include <arpa/inet.h>
#include <ctype.h>
#include <dlfcn.h>
#include <errno.h>
#include <netinet/in.h>
#include <pthread.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <unistd.h>

#define PORT 48771
#define MAX_CMD 192

typedef void Il2CppDomain;
typedef void Il2CppAssembly;
typedef void Il2CppImage;
typedef void Il2CppClass;
typedef void Il2CppMethod;
typedef void Il2CppType;
typedef void Il2CppObject;
typedef void Il2CppField;
typedef struct { void *klass; void *monitor; int32_t length; uint16_t chars[0]; } Il2CppString;
typedef struct { void *klass; void *monitor; void *bounds; uintptr_t max_length; void *vector[0]; } Il2CppArray;
typedef struct { float x,y,z; } Vec3;

typedef Il2CppDomain* (*fn_domain_get)(void);
typedef const Il2CppAssembly** (*fn_domain_get_assemblies)(const Il2CppDomain*, size_t*);
typedef const Il2CppImage* (*fn_assembly_get_image)(const Il2CppAssembly*);
typedef Il2CppClass* (*fn_class_from_name)(const Il2CppImage*, const char*, const char*);
typedef size_t (*fn_image_get_class_count)(const Il2CppImage*);
typedef Il2CppClass* (*fn_image_get_class)(const Il2CppImage*, size_t);
typedef const char* (*fn_class_get_name)(Il2CppClass*);
typedef const char* (*fn_class_get_namespace)(Il2CppClass*);
typedef const Il2CppMethod* (*fn_class_get_method_from_name)(Il2CppClass*, const char*, int);
typedef const Il2CppType* (*fn_class_get_type)(Il2CppClass*);
typedef Il2CppObject* (*fn_type_get_object)(const Il2CppType*);
typedef Il2CppObject* (*fn_runtime_invoke)(const Il2CppMethod*, void*, void**, Il2CppObject**);
typedef void* (*fn_thread_attach)(Il2CppDomain*);
typedef Il2CppField* (*fn_class_get_field_from_name)(Il2CppClass*, const char*);
typedef void (*fn_field_set_value)(Il2CppObject*, Il2CppField*, void*);
typedef void (*fn_field_static_set_value)(Il2CppField*, void*);
typedef Il2CppString* (*fn_string_new)(const char*);

static void *h_il2cpp;
static fn_domain_get p_domain_get;
static fn_domain_get_assemblies p_domain_get_assemblies;
static fn_assembly_get_image p_assembly_get_image;
static fn_class_from_name p_class_from_name;
static fn_image_get_class_count p_image_get_class_count;
static fn_image_get_class p_image_get_class;
static fn_class_get_name p_class_get_name;
static fn_class_get_namespace p_class_get_namespace;
static fn_class_get_method_from_name p_class_get_method_from_name;
static fn_class_get_type p_class_get_type;
static fn_type_get_object p_type_get_object;
static fn_runtime_invoke p_runtime_invoke;
static fn_thread_attach p_thread_attach;
static fn_class_get_field_from_name p_class_get_field_from_name;
static fn_field_set_value p_field_set_value;
static fn_field_static_set_value p_field_static_set_value;
static fn_string_new p_string_new;

typedef void* (*fn_choreo_get)(void);
typedef void (*fn_choreo_cb)(long, void*);
typedef void (*fn_choreo_post)(void*, fn_choreo_cb, void*);
static void *h_android, *choreo;
static fn_choreo_get p_choreo_get;
static fn_choreo_post p_choreo_post;

static pthread_mutex_t cmd_mu=PTHREAD_MUTEX_INITIALIZER;
static pthread_cond_t cmd_cv=PTHREAD_COND_INITIALIZER;
static char pending_cmd[MAX_CMD];
static int pending=0, done=0, result_code=0;
static int fly_state=0, noclip_state=0;

static int eqi(const char *a,const char *b){
    while(*a&&*b){ if(tolower((unsigned char)*a++)!=tolower((unsigned char)*b++)) return 0; }
    return *a==0&&*b==0;
}
static int contains_i(const char *s,const char *needle){
    if(!s||!needle||!*needle) return 0;
    size_t n=strlen(needle);
    for(;*s;s++){ size_t i=0; while(i<n&&s[i]&&tolower((unsigned char)s[i])==tolower((unsigned char)needle[i])) i++; if(i==n) return 1; }
    return 0;
}

static int il2cpp_ready(void){
    if(h_il2cpp) return 1;
    h_il2cpp=dlopen("libil2cpp.so",RTLD_NOW|RTLD_NOLOAD);
    if(!h_il2cpp) h_il2cpp=dlopen("libil2cpp.so",RTLD_NOW);
    if(!h_il2cpp) return 0;
#define RESOLVE(name) do { p_##name=(fn_##name)dlsym(h_il2cpp,"il2cpp_" #name); if(!p_##name) return 0; } while(0)
    RESOLVE(domain_get); RESOLVE(domain_get_assemblies); RESOLVE(assembly_get_image);
    RESOLVE(class_from_name); RESOLVE(image_get_class_count); RESOLVE(image_get_class);
    RESOLVE(class_get_name); RESOLVE(class_get_namespace); RESOLVE(class_get_method_from_name);
    RESOLVE(class_get_type); RESOLVE(type_get_object); RESOLVE(runtime_invoke); RESOLVE(thread_attach);
    RESOLVE(class_get_field_from_name); RESOLVE(field_set_value); RESOLVE(field_static_set_value);
    RESOLVE(string_new);
#undef RESOLVE
    Il2CppDomain *d=p_domain_get(); if(!d) return 0; p_thread_attach(d); return 1;
}

static Il2CppClass* find_class(const char *ns,const char *name){
    Il2CppDomain *d=p_domain_get(); if(!d) return NULL;
    size_t n=0; const Il2CppAssembly **as=p_domain_get_assemblies(d,&n); if(!as) return NULL;
    for(size_t i=0;i<n;i++){ const Il2CppImage *im=p_assembly_get_image(as[i]); if(!im) continue; Il2CppClass *c=p_class_from_name(im,ns?ns:"",name); if(c) return c; }
    return NULL;
}
static Il2CppClass* find_class_any(const char *name){
    Il2CppClass *c=find_class("",name); if(c) return c;
    Il2CppDomain *d=p_domain_get(); if(!d) return NULL; size_t n=0; const Il2CppAssembly **as=p_domain_get_assemblies(d,&n);
    for(size_t i=0;i<n;i++){ const Il2CppImage *im=p_assembly_get_image(as[i]); if(!im) continue; size_t m=p_image_get_class_count(im); for(size_t j=0;j<m;j++){ c=p_image_get_class(im,j); const char *cn=c?p_class_get_name(c):NULL; if(cn&&strcmp(cn,name)==0) return c; }}
    return NULL;
}

static Il2CppObject* invoke(const Il2CppMethod *m, void *obj, void **args){
    if(!m) return NULL; Il2CppObject *exc=NULL; Il2CppObject *r=p_runtime_invoke(m,obj,args,&exc); return exc?NULL:r;
}
static Il2CppObject* find_one(Il2CppClass *target){
    if(!target) return NULL;
    Il2CppClass *obj=find_class("UnityEngine","Object"); if(!obj) obj=find_class_any("Object");
    if(!obj) return NULL;
    const Il2CppMethod *m=p_class_get_method_from_name(obj,"FindObjectOfType",1); if(!m) return NULL;
    Il2CppObject *type=p_type_get_object(p_class_get_type(target)); if(!type) return NULL;
    void *args[1]={type}; return invoke(m,NULL,args);
}
static Il2CppArray* find_all(Il2CppClass *target){
    if(!target) return NULL;
    Il2CppClass *res=find_class("UnityEngine","Resources"); if(!res) res=find_class_any("Resources");
    if(!res) return NULL;
    const Il2CppMethod *m=p_class_get_method_from_name(res,"FindObjectsOfTypeAll",1); if(!m) return NULL;
    Il2CppObject *type=p_type_get_object(p_class_get_type(target)); if(!type) return NULL;
    void *args[1]={type}; return (Il2CppArray*)invoke(m,NULL,args);
}

static int call0_on_one(const char *cls,const char *method){
    Il2CppClass *c=find_class_any(cls); if(!c) return 0; Il2CppObject *o=find_one(c); if(!o) return 0;
    const Il2CppMethod *m=p_class_get_method_from_name(c,method,0); if(!m) return 0;
    Il2CppObject *exc=NULL; p_runtime_invoke(m,o,NULL,&exc); return exc==NULL;
}
static int call0_on_all(const char *cls,const char *method){
    Il2CppClass *c=find_class_any(cls); if(!c) return 0; const Il2CppMethod *m=p_class_get_method_from_name(c,method,0); if(!m) return 0;
    Il2CppArray *arr=find_all(c); if(!arr||arr->max_length>10000) return 0; int hit=0;
    for(uintptr_t i=0;i<arr->max_length;i++){ void *o=arr->vector[i]; if(!o) continue; Il2CppObject *exc=NULL; p_runtime_invoke(m,o,NULL,&exc); if(!exc) hit++; }
    return hit>0;
}
static int call1i(const char *cls,const char *method,int v){
    Il2CppClass *c=find_class_any(cls); if(!c) return 0; Il2CppObject *o=find_one(c); if(!o) return 0;
    const Il2CppMethod *m=p_class_get_method_from_name(c,method,1); if(!m) return 0; void *args[1]={&v}; Il2CppObject *exc=NULL; p_runtime_invoke(m,o,args,&exc); return exc==NULL;
}

static int set_menu_mode(int on){
    Il2CppClass *c=find_class_any("ModeMenuButton"); if(!c) return 0;
    Il2CppField *f=p_class_get_field_from_name(c,"isOnMenuMode"); if(f) p_field_static_set_value(f,&on);
    return f!=NULL;
}
static int apply_fp(void){
    Il2CppClass *c=find_class_any("FP_Mode"); if(!c) return 0; Il2CppObject *o=find_one(c); if(!o) return 0;
    Il2CppField *ff=p_class_get_field_from_name(c,"fly_Mode");
    Il2CppField *fn=p_class_get_field_from_name(c,"noClip_Mode");
    if(!ff||!fn) return 0;
    p_field_set_value(o,ff,&fly_state); p_field_set_value(o,fn,&noclip_state);
    if(fly_state||noclip_state) set_menu_mode(1);
    return 1;
}
static int open_builtin_menu(void){
    if(call0_on_one("ModeMenuButton","ToggleIsMenuMode")) return 1;
    return set_menu_mode(1);
}

static const char* u16_to_ascii(Il2CppString *s,char *out,size_t cap){
    if(!s||cap<2){ if(cap) out[0]=0; return out; }
    int32_t n=s->length; if(n<0) n=0; size_t k=0;
    for(int32_t i=0;i<n&&k+1<cap;i++){ uint16_t ch=s->chars[i]; out[k++]=(ch<128)?(char)ch:'?'; }
    out[k]=0; return out;
}
static const char* object_name(void *obj,char *buf,size_t cap){
    Il2CppClass *oc=find_class("UnityEngine","Object"); if(!oc) oc=find_class_any("Object"); if(!oc) return NULL;
    const Il2CppMethod *m=p_class_get_method_from_name(oc,"get_name",0); if(!m) return NULL;
    Il2CppString *s=(Il2CppString*)invoke(m,obj,NULL); if(!s) return NULL; return u16_to_ascii(s,buf,cap);
}

static Il2CppObject* gameobject_find(const char *name){
    Il2CppClass *gc=find_class("UnityEngine","GameObject"); if(!gc) gc=find_class_any("GameObject"); if(!gc) return NULL;
    const Il2CppMethod *m=p_class_get_method_from_name(gc,"Find",1); if(!m) return NULL;
    Il2CppString *s=p_string_new(name); void *args[1]={s}; return invoke(m,NULL,args);
}
static int set_go_scale(Il2CppObject *go,float x,float y,float z){
    if(!go) return 0; Il2CppClass *gc=find_class("UnityEngine","GameObject"); if(!gc) gc=find_class_any("GameObject");
    Il2CppClass *tc=find_class("UnityEngine","Transform"); if(!tc) tc=find_class_any("Transform"); if(!gc||!tc) return 0;
    const Il2CppMethod *gm=p_class_get_method_from_name(gc,"get_transform",0);
    const Il2CppMethod *sm=p_class_get_method_from_name(tc,"set_localScale",1); if(!gm||!sm) return 0;
    Il2CppObject *tr=invoke(gm,go,NULL); if(!tr) return 0; Vec3 v={x,y,z}; void *args[1]={&v}; Il2CppObject *exc=NULL; p_runtime_invoke(sm,tr,args,&exc); return exc==NULL;
}
static int scale_npc(const char *who,float x,float y,float z){
    const char *names[10]={0}; int count=0;
    if(eqi(who,"MOM")){ const char *a[]={"Mom","Mom1","Mom2","Mom3","Mom4","Mom(Clone)"}; count=6; for(int i=0;i<count;i++) names[i]=a[i]; }
    else if(eqi(who,"DAD")){ const char *a[]={"Dad","Dad1","Dad2","Dad(Clone)"}; count=4; for(int i=0;i<count;i++) names[i]=a[i]; }
    else if(eqi(who,"DOG")){ const char *a[]={"Dog","Dog1","Dog(Clone)"}; count=3; for(int i=0;i<count;i++) names[i]=a[i]; }
    else if(eqi(who,"ANDREW")){ const char *a[]={"Andrew","Andrew1","Igor","Igor1"}; count=4; for(int i=0;i<count;i++) names[i]=a[i]; }
    int hit=0; for(int i=0;i<count;i++){ Il2CppObject *go=gameobject_find(names[i]); if(go&&set_go_scale(go,x,y,z)) hit++; }
    return hit>0;
}

static int spawn_transport(const char *kind){
    Il2CppClass *c=find_class_any("SpawnPrefab"); if(!c) return 0;
    const Il2CppMethod *spawn=p_class_get_method_from_name(c,"Spawn",0); if(!spawn) return 0;
    Il2CppArray *arr=find_all(c); if(!arr||arr->max_length>5000) return 0;
    Il2CppClass *comp=find_class("UnityEngine","Component"); if(!comp) comp=find_class_any("Component");
    const Il2CppMethod *gg=comp?p_class_get_method_from_name(comp,"get_gameObject",0):NULL;
    int hit=0;
    for(uintptr_t i=0;i<arr->max_length;i++){
        void *component=arr->vector[i]; if(!component) continue; char nm[160]=""; void *go=gg?invoke(gg,component,NULL):NULL; if(go) object_name(go,nm,sizeof(nm));
        int match=0;
        if(eqi(kind,"CAR")) match=contains_i(nm,"car")||contains_i(nm,"auto")||contains_i(nm,"vehicle");
        else if(eqi(kind,"VAN")) match=contains_i(nm,"van")||contains_i(nm,"bus");
        else if(eqi(kind,"BIKE")) match=contains_i(nm,"bike")||contains_i(nm,"bicycle");
        else if(eqi(kind,"SCOOTER")) match=contains_i(nm,"scooter")||contains_i(nm,"kick");
        else if(eqi(kind,"ANY")) match=1;
        if(match){ Il2CppObject *exc=NULL; p_runtime_invoke(spawn,component,NULL,&exc); if(!exc){ hit++; break; } }
    }
    if(hit==0&&arr->max_length>0){ for(uintptr_t i=0;i<arr->max_length;i++){ if(!arr->vector[i]) continue; Il2CppObject *exc=NULL; p_runtime_invoke(spawn,arr->vector[i],NULL,&exc); if(!exc){ hit=1; break; } } }
    return hit>0;
}

static int play_as_mom(void){ return call0_on_one("NpcSettings","OnStartSelectedStateMomClicked"); }
static int do_front(void){ int ok=0; ok|=call0_on_all("KnockingDoor","UnlockedExit"); ok|=call0_on_all("Frank","ToggleOpenMainDoor"); return ok; }
static int do_basement(void){ int ok=0; ok|=call0_on_all("FrankBasementState","Unlock"); ok|=call0_on_all("FrankBasementState","OpenDoor"); return ok; }

static int execute_cmd(const char *cmd){
    if(!il2cpp_ready()) return 2;
    if(strncmp(cmd,"ITEM:",5)==0){ int idx=atoi(cmd+5); return call1i("SpawnItems","SpawnItemAtIndex",idx)?1:0; }
    if(strncmp(cmd,"FLY:",4)==0){ fly_state=atoi(cmd+4)?1:0; return apply_fp()?1:0; }
    if(strncmp(cmd,"LEV:",4)==0){ fly_state=atoi(cmd+4)?1:0; return apply_fp()?1:0; }
    if(strncmp(cmd,"NOCLIP:",7)==0){ noclip_state=atoi(cmd+7)?1:0; if(noclip_state) fly_state=1; return apply_fp()?1:0; }
    if(strncmp(cmd,"TYSON:",6)==0){ int on=atoi(cmd+6); int ok=0; if(on){ ok|=call0_on_all("Mom","StopMom"); ok|=call0_on_all("Dad","StopDad"); } else { ok|=call0_on_all("Mom","MomReset"); ok|=call0_on_all("Dad","DadReset"); } return ok?1:0; }
    if(strcmp(cmd,"MENU")==0) return open_builtin_menu()?1:0;
    if(strcmp(cmd,"PLAYMOM")==0) return play_as_mom()?1:0;
    if(strcmp(cmd,"MOM")==0) return call0_on_all("Mom","ForceDetectPlayer")?1:0;
    if(strcmp(cmd,"DAD")==0) return call0_on_all("Dad","ForceDetectPlayer")?1:0;
    if(strcmp(cmd,"SECRET")==0) return call0_on_all("CodeLock","SkipPassword")?1:0;
    if(strcmp(cmd,"SAFE")==0) return call0_on_all("SafeColorLock","SkipPassword")?1:0;
    if(strcmp(cmd,"FRONT")==0) return do_front()?1:0;
    if(strcmp(cmd,"BASEMENT")==0) return do_basement()?1:0;
    if(strncmp(cmd,"SCALE:",6)==0){ char who[24]={0}; float x=1,y=1,z=1; if(sscanf(cmd+6,"%23[^:]:%f:%f:%f",who,&x,&y,&z)==4) return scale_npc(who,x,y,z)?1:0; return 0; }
    if(strncmp(cmd,"SPAWN:",6)==0) return spawn_transport(cmd+6)?1:0;
    return 3;
}

static void frame_cb(long frameTimeNanos, void *data){
    (void)frameTimeNanos; (void)data;
    char cmd[MAX_CMD]={0}; int has=0;
    pthread_mutex_lock(&cmd_mu); if(pending){ strncpy(cmd,pending_cmd,sizeof(cmd)-1); pending=0; has=1; } pthread_mutex_unlock(&cmd_mu);
    if(has){ int r=execute_cmd(cmd); pthread_mutex_lock(&cmd_mu); result_code=r; done=1; pthread_cond_broadcast(&cmd_cv); pthread_mutex_unlock(&cmd_mu); }
    if(p_choreo_post&&choreo) p_choreo_post(choreo,frame_cb,NULL);
}

static const char* result_text(int code){ if(code==1) return "OK"; if(code==2) return "NOT_READY"; if(code==3) return "BAD_CMD"; return "FAIL"; }
static void* server_thread(void *unused){
    (void)unused;
    int s=socket(AF_INET,SOCK_STREAM,0); if(s<0) return NULL; int one=1; setsockopt(s,SOL_SOCKET,SO_REUSEADDR,&one,sizeof(one));
    struct sockaddr_in a; memset(&a,0,sizeof(a)); a.sin_family=AF_INET; a.sin_port=htons(PORT); a.sin_addr.s_addr=htonl(INADDR_LOOPBACK);
    if(bind(s,(struct sockaddr*)&a,sizeof(a))<0){ close(s); return NULL; } if(listen(s,8)<0){ close(s); return NULL; }
    for(;;){ int c=accept(s,NULL,NULL); if(c<0) continue; struct timeval tv={4,0}; setsockopt(c,SOL_SOCKET,SO_RCVTIMEO,&tv,sizeof(tv)); setsockopt(c,SOL_SOCKET,SO_SNDTIMEO,&tv,sizeof(tv));
        char cmd[MAX_CMD]={0}; ssize_t n=read(c,cmd,sizeof(cmd)-1); if(n<=0){ close(c); continue; } cmd[n]=0; char *e=strpbrk(cmd,"\r\n"); if(e) *e=0;
        if(strcmp(cmd,"PING")==0){ write(c,"PONGB1",6); close(c); continue; }
        pthread_mutex_lock(&cmd_mu);
        if(pending || (!done && pending_cmd[0])){ pthread_mutex_unlock(&cmd_mu); write(c,"BUSY",4); close(c); continue; }
        strncpy(pending_cmd,cmd,sizeof(pending_cmd)-1); pending=1; done=0;
        struct timespec ts; clock_gettime(CLOCK_REALTIME,&ts); ts.tv_sec+=3;
        int waiterr=0; while(!done&&waiterr!=ETIMEDOUT) waiterr=pthread_cond_timedwait(&cmd_cv,&cmd_mu,&ts);
        if(done){ int r=result_code; pending_cmd[0]=0; done=0; pthread_mutex_unlock(&cmd_mu); const char *t=result_text(r); write(c,t,strlen(t)); }
        else { pending=0; pending_cmd[0]=0; pthread_mutex_unlock(&cmd_mu); write(c,"NOT_READY",9); }
        close(c);
    }
    return NULL;
}

__attribute__((visibility("default"))) void lambocheat_init(void){
    static int started=0; if(started) return; started=1;
    h_android=dlopen("libandroid.so",RTLD_NOW); if(h_android){ p_choreo_get=(fn_choreo_get)dlsym(h_android,"AChoreographer_getInstance"); p_choreo_post=(fn_choreo_post)dlsym(h_android,"AChoreographer_postFrameCallback"); if(p_choreo_get&&p_choreo_post){ choreo=p_choreo_get(); if(choreo) p_choreo_post(choreo,frame_cb,NULL); }}
    pthread_t t; if(pthread_create(&t,NULL,server_thread,NULL)==0) pthread_detach(t);
}
