import com.comsol.model.*;
import com.comsol.model.util.*;

public class MembraneKevlar {

    private static final String MESH_ID = "mesh";
    private static final String GEOM_ID = "geometry";
    private static final String MATERIAL_ID = "material";

    private static void createCoordSystem(ComponentCoordsysList coordsysList) {
        Coordsys shellSys = coordsysList.create("shellsys", "VectorBase");
        shellSys.label("Local system");
        shellSys.set("orthonormal", true);
        shellSys.set("coord", new String[][]{{"x", "y", "z"}});
        shellSys.set("base", new String[][]{{"root.comp1.shell.dsdX11", "root.comp1.shell.dsdX12", "root.comp1.shell.dsdX13"}, {"root.comp1.shell.dsdX21", "root.comp1.shell.dsdX22", "root.comp1.shell.dsdX23"}, {"root.comp1.shell.dsdX31", "root.comp1.shell.dsdX32", "root.comp1.shell.dsdX33"}});

        coordsysList.get("sys1").label("Boundaries");
    }

    private static void createGeometry(GeomList geomList) {
        GeomSequence geom = geomList.create(GEOM_ID, 3);
        geom.label("Geometry");

        GeomFeature membrane = geom.create("membrane", "ParametricSurface");
        membrane.label("Membrane");
        membrane.set("parmin1", -10);
        membrane.set("parmax1", 10);
        membrane.set("parmin2", -10);
        membrane.set("parmax2", 10);
        membrane.set("coord", new String[]{"s1", "s2", "0"});

        GeomFeature impact = geom.create("impact", "ParametricCurve");
        impact.label("Impact");
        impact.set("parmax", "2*pi");
        impact.set("coord", new String[]{"sin(s)*0.2", "cos(s)*0.2", "0"});

        GeomFeature fin = geom.create("fin", "Finalize");
        fin.label("Full geometry");

        geom.run();
    }

    private static void createMesh(ComponentMeshList meshList) {
        MeshSequence mesh = meshList.create(MESH_ID);
        mesh.label("Mesh");

        MeshFeature size = mesh.feature("size");
        size.set("hauto", 2);
        size.set("custom", "on");
        size.set("hmax", 0.2);
        size.set("hmin", 0.1);

        MeshFeature fq1 = mesh.create("fq1", "FreeQuad");
        fq1.selection().geom(GEOM_ID);
        fq1.label("Free Quad");
        fq1.set("method", "legacy52a");

        mesh.run();
    }

    private static void createMaterials(ComponentMaterialList materials) {
        Material material = materials.create(MATERIAL_ID, "Common");
        material.label("Kevlar 49 [solid,in air]");

        MaterialModel def = material.propertyGroup("def");

        def.func().create("Syt_solid_in_air_2", "Piecewise");
        def.func("Syt_solid_in_air_2").set("arg", "T");
        def.func("Syt_solid_in_air_2").set("pieces", new String[][]{{"273.0", "473.0", "4.798114E9-4319428.0*T^1"}});

        def.func().create("alpha", "Piecewise");
        def.func("alpha").set("arg", "T");
        def.func("alpha").set("pieces", new String[][]{{"293.0", "433.0", "-4.47259E-6-3.807924E-9*T^1"}});

        def.func().create("C", "Piecewise");
        def.func("C").set("arg", "T");
        def.func("C").set("pieces", new String[][]{{"293.0", "453.0", "-2005.021+14.45144*T^1-0.009897633*T^2"}});

        def.func().create("rho", "Piecewise");
        def.func("rho").set("arg", "T");
        def.func("rho").set("pieces", new String[][]{{"293.0", "433.0", "1434.37+0.01429221*T^1+1.680031E-5*T^2"}});

        def.set("Syt", "Syt_solid_in_air_2(T[1/K])[Pa]");
        def.set("thermalexpansioncoefficient", new String[]{"(alpha(T[1/K])[1/K]+(Tempref-293[K])*if(abs(T-Tempref)>1e-3,(alpha(T[1/K])[1/K]-alpha(Tempref[1/K])[1/K])/(T-Tempref),d(alpha(T[1/K])[1/K],T)))/(1+alpha(Tempref[1/K])[1/K]*(Tempref-293[K]))", "0", "0", "0", "(alpha(T[1/K])[1/K]+(Tempref-293[K])*if(abs(T-Tempref)>1e-3,(alpha(T[1/K])[1/K]-alpha(Tempref[1/K])[1/K])/(T-Tempref),d(alpha(T[1/K])[1/K],T)))/(1+alpha(Tempref[1/K])[1/K]*(Tempref-293[K]))", "0", "0", "0", "(alpha(T[1/K])[1/K]+(Tempref-293[K])*if(abs(T-Tempref)>1e-3,(alpha(T[1/K])[1/K]-alpha(Tempref[1/K])[1/K])/(T-Tempref),d(alpha(T[1/K])[1/K],T)))/(1+alpha(Tempref[1/K])[1/K]*(Tempref-293[K]))"});
        def.set("heatcapacity", "C(T[1/K])[J/(kg*K)]");
        def.set("density", "rho(T[1/K])[kg/m^3]");

        def.addInput("temperature");
        def.addInput("strainreferencetemperature");


        MaterialModel thermalExpansion = material.propertyGroup().create("ThermalExpansion", "Thermal expansion");

        thermalExpansion.func().create("dL", "Piecewise");
        thermalExpansion.func("dL").set("arg", "T");
        thermalExpansion.func("dL").set("pieces", new String[][]{{"293.0", "433.0", "0.001310523-3.357184E-6*T^1-3.807471E-9*T^2"}});

        thermalExpansion.func().create("CTE", "Piecewise");
        thermalExpansion.func("CTE").set("arg", "T");
        thermalExpansion.func("CTE").set("pieces", new String[][]{{"293.0", "433.0", "-3.357184E-6-7.614942E-9*T^1"}});

        thermalExpansion.set("alphatan", new String[]{"CTE(T[1/K])[1/K]", "0", "0", "0", "CTE(T[1/K])[1/K]", "0", "0", "0", "CTE(T[1/K])[1/K]"});
        thermalExpansion.set("alphatanIso", "CTE(T)");
        thermalExpansion.set("dL", new String[]{"(dL(T[1/K])-dL(Tempref[1/K]))/(1+dL(Tempref[1/K]))", "0", "0", "0", "(dL(T[1/K])-dL(Tempref[1/K]))/(1+dL(Tempref[1/K]))", "0", "0", "0", "(dL(T[1/K])-dL(Tempref[1/K]))/(1+dL(Tempref[1/K]))"});
        thermalExpansion.set("dLIso", "(dL(T)-dL(Tempref))/(1+dL(Tempref))");

        thermalExpansion.addInput("temperature");
        thermalExpansion.addInput("strainreferencetemperature");


        MaterialModel enu = material.propertyGroup().create("Enu", "Young's modulus and Poisson's ratio");

        enu.func().create("E_solid_in_air_2", "Piecewise");
        enu.func("E_solid_in_air_2").set("arg", "T");
        enu.func("E_solid_in_air_2").set("pieces", new String[][]{{"273.0", "473.0", "4.338668E11-2.146288E9*T^1+4844299.0*T^2-3712.089*T^3"}});

        enu.set("youngsmodulus", "E_solid_in_air_2(T[1/K])[Pa]");
        enu.set("poissonsratio", "0.3");

        enu.addInput("temperature");
    }

    public static Model run() {
        Model model = ModelUtil.create("Model");
        ModelNode component = model.component().create("comp1", false);
        component.label("Main Component");

        component.view("view1").label("View");

        createCoordSystem(component.coordSystem());

        createGeometry(component.geom());
        createMesh(component.mesh());
        createMaterials(component.material());


        component.physics().create("shell", "Shell", GEOM_ID);
        component.physics("shell").create("vel1", "Velocity2", 2);
        component.physics("shell").feature("vel1").selection().set(2);

        model.result().table().create("evl3", "Table");
        model.result().table("evl3").label("Evaluation 3D");
        model.result().table("evl3").comments("Interactive 3D values");

        model.capeopen().label("Thermodynamics Package");

        component.physics("shell").feature("emm1").label("Linear elastic material");
        component.physics("shell").feature("free1").label("Free boundaries");
        component.physics("shell").feature("init1").label("Initial values");
        component.physics("shell").feature("vel1").set("v", new int[][]{{0}, {0}, {5}});
        component.physics("shell").feature("vel1").set("Direction", new int[][]{{0}, {0}, {1}});
        component.physics("shell").feature("vel1").label("Impact velocity");

        model.study().create("std1");
        model.study("std1").create("time", "Transient");
        model.study("std1").feature("time").set("activate", new String[]{"shell", "on"});

        model.sol().create("sol1");
        model.sol("sol1").study("std1");
        model.sol("sol1").attach("std1");
        model.sol("sol1").create("st1", "StudyStep");
        model.sol("sol1").create("v1", "Variables");
        model.sol("sol1").create("t1", "Time");
        model.sol("sol1").feature("t1").create("fc1", "FullyCoupled");
        model.sol("sol1").feature("t1").feature().remove("fcDef");

        model.result().create("pg1", "PlotGroup3D");
        model.result().create("pg2", "PlotGroup3D");
        model.result().create("pg3", "PlotGroup1D");
        model.result("pg1").create("surf1", "Surface");
        model.result("pg1").feature("surf1").create("def", "Deform");
        model.result("pg2").create("surf1", "Surface");
        model.result("pg2").create("surf2", "Surface");
        model.result("pg2").create("arws1", "ArrowSurface");
        model.result("pg2").create("arws2", "ArrowSurface");
        model.result("pg2").feature("surf1").create("def", "Deform");
        model.result("pg2").feature("surf2").create("def", "Deform");
        model.result("pg2").feature("arws1").create("def", "Deform");
        model.result("pg2").feature("arws2").create("def", "Deform");

        model.study("std1").label("Study");
        model.study("std1").feature("time").set("tlist", "range(0,0.03,0.3)");
        model.study("std1").feature("time").set("plot", true);
        model.study("std1").feature("time").set("discretization", new String[]{"shell", "physics"});

        model.sol("sol1").attach("std1");
        model.sol("sol1").feature("v1").set("clistctrl", new String[]{"t1_t"});
        model.sol("sol1").feature("v1").set("cname", new String[]{"t"});
        model.sol("sol1").feature("v1").set("clist", new String[]{"range(0,0.03,0.3)"});
        model.sol("sol1").feature("v1").feature("comp1_u").set("scalemethod", "manual");
        model.sol("sol1").feature("v1").feature("comp1_u").set("scaleval", "1e-2*28.284271247461913");
        model.sol("sol1").feature("t1").set("tlist", "range(0,0.03,0.3)");
        model.sol("sol1").feature("t1").set("atolglobalvaluemethod", "manual");
        model.sol("sol1").feature("t1")
                .set("atolvaluemethod", new String[]{"comp1_ar", "manual", "comp1_shell_vel1_u0", "manual", "comp1_u", "manual"});
        model.sol("sol1").feature("t1").set("timemethod", "genalpha");
        model.sol("sol1").feature("t1").set("plot", true);
        model.sol("sol1").feature("t1").feature("dDef").set("ooc", false);
        model.sol("sol1").runAll();

        model.result().dataset("dset1").label("Displacement");
        model.result("pg1").label("Stress (shell)");
        model.result("pg1").set("solnum", 1);
        model.result("pg1").feature("surf1").label("Surface");
        model.result("pg1").feature("surf1").set("expr", "w");
        model.result("pg1").feature("surf1").set("descr", "Displacement field, z component");
        model.result("pg1").feature("surf1")
                .set("const", new String[][]{{"shell.z", "1", "Local z-coordinate [-1,1] for thickness-dependent results"},
                        {"shell.refpntx", "0", "Reference point for moment computation, x coordinate"},
                        {"shell.refpnty", "0", "Reference point for moment computation, y coordinate"},
                        {"shell.refpntz", "0", "Reference point for moment computation, z coordinate"}});
        model.result("pg1").feature("surf1").set("rangecoloractive", true);
        model.result("pg1").feature("surf1").set("rangecolormax", 0.1);
        model.result("pg1").feature("surf1").set("smooth", "internal");
        model.result("pg1").feature("surf1").set("resolution", "normal");
        model.result("pg1").feature("surf1").feature("def").set("scale", "1.0");
        model.result("pg1").feature("surf1").feature("def").set("scaleactive", true);
        model.result("pg2").label("Undeformed Geometry (shell)");
        model.result("pg2").set("solnum", 3);
        model.result("pg2").set("titletype", "manual");
        model.result("pg2").set("title", "Undeformed Geometry (shell)");
        model.result("pg2").feature("surf1").label("Top Surface (Gray)");
        model.result("pg2").feature("surf1").set("expr", "1");
        model.result("pg2").feature("surf1").set("unit", "1");
        model.result("pg2").feature("surf1").set("descr", "1");
        model.result("pg2").feature("surf1")
                .set("const", new String[][]{{"shell.z", "1", "Local z-coordinate [-1,1] for thickness-dependent results"},
                        {"shell.refpntx", "0", "Reference point for moment computation, x coordinate"},
                        {"shell.refpnty", "0", "Reference point for moment computation, y coordinate"},
                        {"shell.refpntz", "0", "Reference point for moment computation, z coordinate"}});
        model.result("pg2").feature("surf1").set("coloring", "uniform");
        model.result("pg2").feature("surf1").set("color", "gray");
        model.result("pg2").feature("surf1").set("smooth", "internal");
        model.result("pg2").feature("surf1").set("resolution", "normal");
        model.result("pg2").feature("surf1").feature("def")
                .set("expr", new String[]{"shell.nlx*(shell.z_offset+0.5*shell.d)", "shell.nly*(shell.z_offset+0.5*shell.d)", "shell.nlz*(shell.z_offset+0.5*shell.d)"});
        model.result("pg2").feature("surf1").feature("def").set("descr", "");
        model.result("pg2").feature("surf1").feature("def").set("scaleactive", true);
        model.result("pg2").feature("surf2").label("Bottom Surface (Yellow)");
        model.result("pg2").feature("surf2").set("expr", "1");
        model.result("pg2").feature("surf2").set("unit", "1");
        model.result("pg2").feature("surf2").set("descr", "1");
        model.result("pg2").feature("surf2")
                .set("const", new String[][]{{"shell.z", "-1", "Local z-coordinate [-1,1] for thickness-dependent results"},
                        {"shell.refpntx", "0", "Reference point for moment computation, x coordinate"},
                        {"shell.refpnty", "0", "Reference point for moment computation, y coordinate"},
                        {"shell.refpntz", "0", "Reference point for moment computation, z coordinate"}});
        model.result("pg2").feature("surf2").set("coloring", "uniform");
        model.result("pg2").feature("surf2").set("color", "yellow");
        model.result("pg2").feature("surf2").set("smooth", "internal");
        model.result("pg2").feature("surf2").set("resolution", "normal");
        model.result("pg2").feature("surf2").feature("def")
                .set("expr", new String[]{"shell.nlx*(shell.z_offset-0.5*shell.d)", "shell.nly*(shell.z_offset-0.5*shell.d)", "shell.nlz*(shell.z_offset-0.5*shell.d)"});
        model.result("pg2").feature("surf2").feature("def").set("descr", "");
        model.result("pg2").feature("surf2").feature("def").set("scaleactive", true);
        model.result("pg2").feature("arws1").label("Local X-Direction (Red)");
        model.result("pg2").feature("arws1").set("expr", new String[]{"shell.dsdX11", "shell.dsdX12", "shell.dsdX13"});
        model.result("pg2").feature("arws1").set("descr", "");
        model.result("pg2").feature("arws1")
                .set("const", new String[][]{{"shell.z", "1", "Local z-coordinate [-1,1] for thickness-dependent results"},
                        {"shell.refpntx", "0", "Reference point for moment computation, x coordinate"},
                        {"shell.refpnty", "0", "Reference point for moment computation, y coordinate"},
                        {"shell.refpntz", "0", "Reference point for moment computation, z coordinate"}});
        model.result("pg2").feature("arws1").set("scale", 0.8232921950104376);
        model.result("pg2").feature("arws1").set("scaleactive", false);
        model.result("pg2").feature("arws1").feature("def")
                .set("expr", new String[]{"shell.nlx*(shell.z_offset+0.5*shell.d)", "shell.nly*(shell.z_offset+0.5*shell.d)", "shell.nlz*(shell.z_offset+0.5*shell.d)"});
        model.result("pg2").feature("arws1").feature("def").set("descr", "");
        model.result("pg2").feature("arws1").feature("def").set("scaleactive", true);
        model.result("pg2").feature("arws2").label("Local Y-Direction (Green)");
        model.result("pg2").feature("arws2").set("expr", new String[]{"shell.dsdX21", "shell.dsdX22", "shell.dsdX23"});
        model.result("pg2").feature("arws2").set("descr", "");
        model.result("pg2").feature("arws2")
                .set("const", new String[][]{{"shell.z", "1", "Local z-coordinate [-1,1] for thickness-dependent results"},
                        {"shell.refpntx", "0", "Reference point for moment computation, x coordinate"},
                        {"shell.refpnty", "0", "Reference point for moment computation, y coordinate"},
                        {"shell.refpntz", "0", "Reference point for moment computation, z coordinate"}});
        model.result("pg2").feature("arws2").set("scale", 0.8232921950104373);
        model.result("pg2").feature("arws2").set("color", "green");
        model.result("pg2").feature("arws2").set("scaleactive", false);
        model.result("pg2").feature("arws2").feature("def")
                .set("expr", new String[]{"shell.nlx*(shell.z_offset+0.5*shell.d)", "shell.nly*(shell.z_offset+0.5*shell.d)", "shell.nlz*(shell.z_offset+0.5*shell.d)"});
        model.result("pg2").feature("arws2").feature("def").set("descr", "");
        model.result("pg2").feature("arws2").feature("def").set("scaleactive", true);
        model.result("pg3").label("Over line");


        return model;
    }

    public static void main(String[] args) {
        run();
    }

}
