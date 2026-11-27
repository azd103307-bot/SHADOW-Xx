using UnrealBuildTool;

public class UnrealGlowBalls : ModuleRules
{
    public UnrealGlowBalls(ReadOnlyTargetRules Target) : base(Target)
    {
        PCHUsage = PCHUsageMode.UseExplicitOrSharedPCHs;
        PublicDependencyModuleNames.AddRange(new string[]
        {
            "Core", "CoreUObject", "Engine", "InputCore"
        });
    }
}