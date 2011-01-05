public enum ArchbotType { BUILDER, SOLDIER, TRANSPORT, LTOWER, HTOWER, STOWER }
public class Archbot {
    // Initialize variables
    private final RobotControler myRC;
    public ArchbotType type;

    // Constructors
    public Robot(RobotController rc) {
        myRC = rc;
    }
    public void init() {
        ComponentController [] components = myRC.components();

        ident:
        while(true) {
            for(ComponentController comp : components) {
                switch(comp.type()) {
                    case ComponentType.CONSTRUCTOR:
                        type = ArchbotType.BUILDER;
                        break ident;
                }
            }
            myRC.yield();
            components = myRC.newComponents();
        }
        // I now know what I am, I need to start doing stuff.
    }



}
