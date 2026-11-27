#include "GlowOrbPickup.h"
#include "GlowBallPawn.h"
#include "GlowBallGameMode.h"
#include "Components/SphereComponent.h"
#include "Components/StaticMeshComponent.h"
#include "Components/PointLightComponent.h"
#include "UObject/ConstructorHelpers.h"

AGlowOrbPickup::AGlowOrbPickup()
{
    PrimaryActorTick.bCanEverTick = false;

    Trigger = CreateDefaultSubobject<USphereComponent>(TEXT("PickupTrigger"));
    SetRootComponent(Trigger);
    Trigger->InitSphereRadius(115.0f);
    Trigger->SetCollisionEnabled(ECollisionEnabled::QueryOnly);
    Trigger->SetCollisionObjectType(ECC_WorldDynamic);
    Trigger->SetCollisionResponseToAllChannels(ECR_Ignore);
    Trigger->SetCollisionResponseToChannel(ECC_PhysicsBody, ECR_Overlap);
    Trigger->SetGenerateOverlapEvents(true);

    OrbMesh = CreateDefaultSubobject<UStaticMeshComponent>(TEXT("OrbMesh"));
    OrbMesh->SetupAttachment(Trigger);
    OrbMesh->SetCollisionEnabled(ECollisionEnabled::NoCollision);
    static ConstructorHelpers::FObjectFinder<UStaticMesh> SphereAsset(TEXT("/Engine/BasicShapes/Sphere.Sphere"));
    if (SphereAsset.Succeeded())
    {
        OrbMesh->SetStaticMesh(SphereAsset.Object);
        OrbMesh->SetRelativeScale3D(FVector(0.34f));
    }

    OrbLight = CreateDefaultSubobject<UPointLightComponent>(TEXT("OrbLight"));
    OrbLight->SetupAttachment(Trigger);
    OrbLight->SetIntensity(5200.0f);
    OrbLight->SetAttenuationRadius(480.0f);
    OrbLight->SetLightColor(FLinearColor(0.12f, 0.95f, 1.0f));
}

void AGlowOrbPickup::BeginPlay()
{
    Super::BeginPlay();
    Trigger->OnComponentBeginOverlap.AddDynamic(this, &AGlowOrbPickup::HandleOverlap);
}

void AGlowOrbPickup::HandleOverlap(UPrimitiveComponent* OverlappedComponent, AActor* OtherActor,
    UPrimitiveComponent* OtherComponent, int32 OtherBodyIndex, bool bFromSweep,
    const FHitResult& SweepResult)
{
    if (Cast<AGlowBallPawn>(OtherActor))
    {
        if (AGlowBallGameMode* Mode = GetWorld()->GetAuthGameMode<AGlowBallGameMode>())
        {
            Mode->RegisterOrbCollected();
        }
        Destroy();
    }
}