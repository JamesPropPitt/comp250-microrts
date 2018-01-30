package comp250;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.security.Policy;

import org.jdom.JDOMException;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import ai.RandomBiasedAI;
import ai.abstraction.WorkerRush;
import ai.abstraction.pathfinding.BFSPathFinding;
import ai.core.AI;
import rts.GameState;
import rts.PhysicalGameState;
import rts.PlayerAction;
import rts.Trace;
import rts.TraceEntry;
import rts.units.UnitTypeTable;
import tests.MapGenerator;
import util.XMLWriter;

public class PlaySingleMatch {
	
	private static AI loadAI(String jarPath, String className, UnitTypeTable utt) throws Exception {
		ClassLoader loader = new PluginClassLoader(new File(jarPath).toURI().toURL());
		Class<?> cls = loader.loadClass(className);
		return (AI)cls.getConstructor(UnitTypeTable.class, int.class).newInstance(utt, 100);
	}
	
	private static String getStackTrace(Exception ex) {
		StringWriter writer = new StringWriter();
		ex.printStackTrace(new PrintWriter(writer));
		return writer.toString();
	}
	
	private static JsonValue playMatch(
			String jar1, String className1, 
			String jar2, String className2,
			GameState gs, Trace trace) {
		
		int MAXCYCLES = 5000;
        boolean gameover = false;
        
        JsonObject result = new JsonObject();
		
        AI ai1;
		try {
			ai1 = loadAI(jar1, className1, gs.getUnitTypeTable());
		} catch (Exception e1) {
			result.set("stackTrace", getStackTrace(e1));
			result.set("winner", 2);
			return result;
		}
		
        AI ai2;
		try {
			ai2 = loadAI(jar2, className2, gs.getUnitTypeTable());
		} catch (Exception e1) {
			result.set("stackTrace", getStackTrace(e1));
			result.set("winner", 1);
			return result;
		}

        do{
            PlayerAction pa1;
			try {
				pa1 = ai1.getAction(0, gs);
			} catch (Exception e) {
				result.set("stackTrace", getStackTrace(e));
				result.set("winner", 2);
				return result;
			}
			
            PlayerAction pa2;
			try {
				pa2 = ai2.getAction(1, gs);
			} catch (Exception e) {
				result.set("stackTrace", getStackTrace(e));
				result.set("winner", 1);
				return result;
			}
			
            if (!pa1.isEmpty() || !pa2.isEmpty()) {
            	TraceEntry te = new TraceEntry(gs.getPhysicalGameState().clone(),gs.getTime());
                te.addPlayerAction(pa1.clone());
                te.addPlayerAction(pa2.clone());
                trace.addEntry(te);
            }

            gs.issueSafe(pa1);
            gs.issueSafe(pa2);

            // simulate:
            gameover = gs.cycle();
        }while(!gameover && gs.getTime()<MAXCYCLES);
        
        result.set("winner", gs.winner() + 1);
        result.set("duration", gs.getTime());
        return result;
	}

	public static void main(String[] args) {
		
		Policy.setPolicy(new SandboxSecurityPolicy());
		System.setSecurityManager(new SecurityManager());
		
		String jar1 = args[0];
		String className1 = args[1];
		String jar2 = args[2];
		String className2 = args[3];
		String mapName = args[4];
		String traceName = args[5];
		
		UnitTypeTable utt = new UnitTypeTable();
        
		PhysicalGameState pgs;
		try {
			pgs = PhysicalGameState.load(mapName, utt);
		} catch (JDOMException | IOException e) {
			e.printStackTrace();
			return;
		}
		
        GameState gs = new GameState(pgs, utt);
        
        
        Trace trace = new Trace(utt);
        TraceEntry te = new TraceEntry(gs.getPhysicalGameState().clone(),gs.getTime());
        trace.addEntry(te);
        
        JsonValue result = playMatch(jar1, className1, jar2, className2, gs, trace);

        try {
        	BufferedWriter stdout = new BufferedWriter(new OutputStreamWriter(System.out));
			result.writeTo(stdout);
			stdout.flush();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

        te = new TraceEntry(gs.getPhysicalGameState().clone(), gs.getTime());
        trace.addEntry(te);
        
        XMLWriter xml;
		try {
			xml = new XMLWriter(new FileWriter(traceName));
	        trace.toxml(xml);
	        xml.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
