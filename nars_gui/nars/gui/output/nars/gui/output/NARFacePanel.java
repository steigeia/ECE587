package nars.gui.output;

import automenta.vivisect.face.HumanoidFacePanel;
import nars.NAR;
import nars.config.Parameters;



public class NARFacePanel extends HumanoidFacePanel  {
    private final NAR nar;

    public NARFacePanel(NAR n) {
        super();
        this.nar = n;
    }
    
    @Override
    protected void onShowing(boolean showing) {
        super.onShowing(showing);
        
//        if (showing) {
//            nar.addOutput(this);
//        }
//        else {
//            nar.removeOutput(this);
//        }
    }
    

    @Override
    public void update(double t) {
        happy = nar.memory.emotion.happy() > Parameters.HAPPY_EVENT_HIGHER_THRESHOLD;
        unhappy = nar.memory.emotion.happy() < Parameters.HAPPY_EVENT_LOWER_THRESHOLD;
        float conceptPriority = 0.5f; //((Number)nar.memory.logic.get("concept.priority.mean")).floatValue();
        float taskNewPriority = 0.5f; //((Number)nar.memory.logic.get("task.new.priority.mean")).floatValue();        
        
        //max out at 0.5
        conceptPriority = Math.min(conceptPriority, 0.4f);
        //if (nar.memory.getConcepts().isEmpty())
            conceptPriority = 0; //if no concepts, start at zero, regardless of what mean might be valued
        
        face.setPupil(12f * (conceptPriority+0.35f)+2f,                 
                taskNewPriority*taskNewPriority*0.45f,0,0,0.9f); //pupils glow a little red for priority of new tasks
        
        face.setEyeball(8f * (conceptPriority + 0.35f)+12f,1f,1f,1f,0.85f);
        super.update(t);        
    }

//    @Override
//    public void output(Class channel, Object signal) {
//        talk=1;
//    }
    
}
