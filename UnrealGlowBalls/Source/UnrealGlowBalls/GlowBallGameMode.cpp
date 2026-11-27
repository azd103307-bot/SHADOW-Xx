#include "GlowBallGameMode.h"
#include "GlowBallPawn.h"
#include "Engine/StaticMeshActor.h"
#include "Components/StaticMeshComponent.h"
#include "Engine/DirectionalLight.h"
#include "Components/DirectionalLightComponent.h"
#include "UObject/ConstructorHelpers.h"

AGlowBallGameMode::AGlowBallGameMode()
{
    DefaultPawnClass = AGlowBallPawn::StaticClass();
}

void AGlowBallGameMode::BeginPlay()
{
    Super::BeginPlay();

    UWorld* World = GetWorld();
    if (!World) return;

    // A simple physical arena is created at runtime so the blank starter map is playable.
    AStaticMeshActor* Floor = World->SpawnActor<AStaticMeshActor>(
        FVector(0.0f, 0.0f, -122.0f), FRotator::ZeroRotator);
    if (Floor)
    {
        static ConstructorHelpers::FObjectFinder<UStaticMesh> CubeAsset(TEXT("/Engine/BasicShapes/Cube.Cube"));
        if (CubeAsset.Succeeded())
        {
            Floor->GetStaticMeshComponent()->SetStaticMesh(CubeAsset.Object);
            Floor->SetActorScale3D(FVector(50.0f, 50.0f, 1.0f));
            Floor->GetStaticMeshComponent()->SetMobility(EComponentMobility::Static);
            Floor->GetStaticMeshComponent()->SetCollisionProfileName(TEXT("BlockAll"));
        }
    }

    ADirectionalLight* Sun = World->SpawnActor<ADirectionalLight>(
        FVector(0.0f, 0.0f, 900.0f), FRotator(-48.0f, -32.0f, 0.0f));
    if (Sun)
    {
        Sun->GetLightComponent()->SetIntensity(8.0f);
        Sun->GetLightComponent()->SetLightColor(FLinearColor(0.62f, 0.78f, 1.0f));
    }
}