package games.strategy.triplea.ai.Dynamix_AI.Others;

public class TerritoryStatus {
  public TerritoryStatus() {}

  public boolean WasAttacked_LandGrab = false;
  public boolean WasAttacked_Stabalize = false;
  public boolean WasAttacked_Offensive = false;
  public boolean WasAttacked_Trade = false;
  public boolean WasReinforced_Block = false;
  public boolean WasReinforced_Stabalize = false;
  public boolean WasReinforced_Frontline = false;
  public boolean WasRetreatedFrom = false;

  public boolean WasAttacked() {
    return WasAttacked_LandGrab || WasAttacked_Offensive || WasAttacked_Stabalize || WasAttacked_Trade;
  }

  public boolean WasReinforced() {
    return WasReinforced_Block || WasReinforced_Frontline || WasReinforced_Stabalize;
  }

  public void NotifyTaskPerform(final CM_Task task) {
    if (task.GetTaskType() == CM_TaskType.Land_LandGrab) {
      WasAttacked_LandGrab = true;
    } else if (task.GetTaskType() == CM_TaskType.Land_Attack_Stabilize) {
      WasAttacked_Stabalize = true;
    } else if (task.GetTaskType() == CM_TaskType.Land_Attack_Offensive) {
      WasAttacked_Offensive = true;
    } else if (task.GetTaskType() == CM_TaskType.Land_Attack_Trade) {
      WasAttacked_Trade = true;
    }
  }

  public void NotifyTaskPerform(final NCM_Task task) {
    if (task.GetTaskType() == NCM_TaskType.Land_Reinforce_Block) {
      WasReinforced_Block = true;
    } else if (task.GetTaskType() == NCM_TaskType.Land_Reinforce_FrontLine) {
      WasReinforced_Frontline = true;
    } else if (task.GetTaskType() == NCM_TaskType.Land_Reinforce_Stabilize) {
      WasReinforced_Stabalize = true;
    }
  }
}
