package protomoto.cell;

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;
import protomoto.runtime.Evaluator;
import protomoto.runtime.Frame;
import protomoto.runtime.Instruction;
import protomoto.cell.AbstractCell;
import protomoto.cell.Cell;
import protomoto.runtime.Hotspot;
import protomoto.runtime.Hotspot0;
import protomoto.runtime.HotspotStrategy;
import protomoto.runtime.Jitter;

public class BehaviorCell extends AbstractCell {
    private Cell frameProto;
    private Instruction[] instructions;
    private int variableCount;
    private String[] parameters;

    public BehaviorCell(Cell frameProto, Instruction[] instructions, int variableCount, String[] parameters) {
        this.frameProto = frameProto;
        this.instructions = instructions;
        this.variableCount = variableCount;
        this.parameters = parameters;
    }

    @Override
    public BehaviorCell resolveEvaluateBehavior() {
        return this;
    }

    @Override
    public Cell resolveProto(Environment environment) {
        return environment.getBehaviorProto();
    }
    
    public Frame createSendFrame(Evaluator evaluator, Frame sender, int arity, Cell[] selfAndArguments) {
        Frame frame = new Frame(frameProto, evaluator, sender, instructions);
        
        frame.push(selfAndArguments[0]);
        
        if(arity > 0) {
            frame.pushFrom(1, arity, selfAndArguments);
        }
        
        frame.allocate(variableCount);
        
        return frame;
    }
    
    public Frame createSendFrame(Evaluator evaluator, Frame sender, int arity) {
        Frame frame = new Frame(frameProto, evaluator, sender, instructions);
        
        sender.popInto(1 + arity, frame);
        /*frame.push(selfAndArguments[0]);
        
        if(arity > 0) {
            frame.pushFrom(1, arity, selfAndArguments);
        }*/
        
        frame.allocate(variableCount);
        
        return frame;
    }

    @Override
    public Cell cloneCell() {
        return new BehaviorCell(frameProto, instructions, variableCount, parameters);
    }
    
    private Hotspot hotspot;

    public Hotspot getHotspot(Environment environment, int arity) {
        if(hotspot == null) {
            try {
                Jitter jitter = new Jitter(environment.getHotspotStrategy(), arity);

                jitter.emit(instructions);

                Class<?> hotspotClass = jitter.compileClass();
                hotspot = (Hotspot)hotspotClass.getConstructor(HotspotStrategy.class).newInstance(environment.getHotspotStrategy());
            } catch (InstantiationException ex) {
                Logger.getLogger(BehaviorCell.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(BehaviorCell.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(BehaviorCell.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InvocationTargetException ex) {
                Logger.getLogger(BehaviorCell.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NoSuchMethodException ex) {
                Logger.getLogger(BehaviorCell.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return hotspot;
    }
}
