/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package filesystem.nodes;

/**
 *
 * @author eyden
 */
public class LinkNode extends FSNode {
    private String target;

    public LinkNode(String name, String owner, String group, String target) {
        super(name, owner, group);
        this.target = target;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    @Override
    public boolean isDirectory() {
        return false;
    }
}
