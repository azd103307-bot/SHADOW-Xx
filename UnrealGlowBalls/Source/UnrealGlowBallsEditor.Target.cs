using UnrealBuildTool;
using System.Collections.Generic;

public class UnrealGlowBallsEditorTarget : TargetRules
{
    public UnrealGlowBallsEditorTarget(TargetInfo Target) : base(Target)
    {
        Type = TargetType.Editor;
        DefaultBuildSettings = BuildSettingsVersion.V5;
        IncludeOrderVersion = EngineIncludeOrderVersion.Unreal5_4;
        ExtraModuleNames.Add("UnrealGlowBalls");
    }
}