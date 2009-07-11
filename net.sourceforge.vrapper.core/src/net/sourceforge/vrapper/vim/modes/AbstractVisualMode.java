package net.sourceforge.vrapper.vim.modes;

import static net.sourceforge.vrapper.keymap.StateUtils.union;
import static net.sourceforge.vrapper.keymap.vim.ConstructorWrappers.key;
import static net.sourceforge.vrapper.keymap.vim.ConstructorWrappers.leafBind;
import static net.sourceforge.vrapper.keymap.vim.ConstructorWrappers.state;
import static net.sourceforge.vrapper.vim.commands.ConstructorWrappers.dontRepeat;
import static net.sourceforge.vrapper.vim.commands.ConstructorWrappers.seq;
import net.sourceforge.vrapper.keymap.KeyStroke;
import net.sourceforge.vrapper.keymap.SpecialKey;
import net.sourceforge.vrapper.keymap.State;
import net.sourceforge.vrapper.keymap.vim.CountingState;
import net.sourceforge.vrapper.keymap.vim.RegisterState;
import net.sourceforge.vrapper.keymap.vim.VisualMotionState;
import net.sourceforge.vrapper.utils.CaretType;
import net.sourceforge.vrapper.vim.EditorAdaptor;
import net.sourceforge.vrapper.vim.commands.ChangeModeCommand;
import net.sourceforge.vrapper.vim.commands.ChangeOperation;
import net.sourceforge.vrapper.vim.commands.Command;
import net.sourceforge.vrapper.vim.commands.DeleteOperation;
import net.sourceforge.vrapper.vim.commands.LeaveVisualModeCommand;
import net.sourceforge.vrapper.vim.commands.SelectionBasedTextOperation;
import net.sourceforge.vrapper.vim.commands.SwapSelectionSidesCommand;
import net.sourceforge.vrapper.vim.commands.YankOperation;

public abstract class AbstractVisualMode extends CommandBasedMode {

    public static final String KEYMAP_NAME = "Visual Mode Keymap";

    public AbstractVisualMode(EditorAdaptor editorAdaptor) {
        super(editorAdaptor);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected KeyMapResolver buildKeyMapResolver() {
        State<String> state = union(
//                state(
//                    leafBind('"', KeyMapResolver.NO_KEYMAP)),
                getKeyMapsForMotions(),
                editorAdaptor.getPlatformSpecificStateProvider().getKeyMaps(VisualMode.NAME));
        final State<String> countEater = new CountConsumingState(state);
        State<String> registerKeymapState = new RegisterKeymapState(KEYMAP_NAME, countEater);
        return new KeyMapResolver(registerKeymapState, KEYMAP_NAME);
    }

    @Override
    protected void placeCursor() {
    //        if (!isEnabled) {
    //            Position leftSidePosition = editorAdaptor.getSelection().getLeftBound();
    //            editorAdaptor.setPosition(leftSidePosition, false);
    //        }
        }

    public void enterMode(Object... args) {
        if (isEnabled) {
            return;
        }
        isEnabled = true;
        editorAdaptor.getCursorService().setCaret(CaretType.VERTICAL_BAR);
    }

    @Override
    public void leaveMode() {
        isEnabled = false;
    }

    @Override
    protected State<Command> getInitialState() {
        Command leaveVisual = new LeaveVisualModeCommand();
        Command swapSides = new SwapSelectionSidesCommand();
        Command yank   = dontRepeat(seq(new SelectionBasedTextOperation(new YankOperation()), leaveVisual));
        Command delete = dontRepeat(seq(new SelectionBasedTextOperation(new DeleteOperation()), leaveVisual));
        Command change = new SelectionBasedTextOperation(new ChangeOperation());
        Command commandLineMode = new ChangeModeCommand(CommandLineMode.NAME);
        State<Command> visualMotions = getVisualMotionState();
        @SuppressWarnings("unchecked")
        State<Command> commands = new RegisterState(CountingState.wrap(union(state(
                leafBind(key(KeyStroke.CTRL, '['), leaveVisual),
                leafBind(SpecialKey.ESC, leaveVisual),
                leafBind('v', leaveVisual),
                leafBind('y', yank),
                leafBind('s', change),
                leafBind('c', change),
                leafBind('d', delete),
                leafBind('x', delete),
                leafBind('X', delete),
                leafBind('o', swapSides),
                leafBind(':', commandLineMode)
        ), visualMotions,
        getPlatformSpecificState(VisualMode.NAME))));
        return commands;
    }

    protected abstract VisualMotionState getVisualMotionState();

}