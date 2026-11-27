#include "GlowBallPawn.h"
#include "Components/SphereComponent.h"
#include "Components/StaticMeshComponent.h"
#include "GameFramework/SpringArmComponent.h"
#include "Camera/CameraComponent.h"
#include "Components/PointLightComponent.h"
#include "UObject/ConstructorHelpers.h"
#include "Engine/StaticMesh.h"
#include "GameFramework/PlayerController.h"
#include "Components/InputComponent.h"

AGlowBallPawn::AGlowBallPawn()
{
    PrimaryActorTick.bCanEverTick = true;
    CollisionSphere = CreateDefaultSubobject<USphereComponent>(TEXT("PhysicsSphere"));
    SetRootComponent(CollisionSphere);
    CollisionSphere->InitSphereRadius(72.0f);
    CollisionSphere->SetSimulatePhysics(true);
    CollisionSphere->SetEnableGravity(true);
    CollisionSphere->SetLinearDamping(0.45f);
    CollisionSphere->SetAngularDamping(0.7f);
    CollisionSphere->SetCollisionProfileName(TEXT("PhysicsActor"));
    CollisionSphere->SetMassOverrideInKg(NAME_None, 45.0f, true);

    BallMesh = CreateDefaultSubobject<UStaticMeshComponent>(TEXT("BallVisual"));
    BallMesh->SetupAttachment(CollisionSphere);
    BallMesh->SetCollisionEnabled(ECollisionEnabled::NoCollision);
    static ConstructorHelpers::FObjectFinder<UStaticMesh> SphereAsset(TEXT("/Engine/BasicShapes/Sphere.Sphere"));
    if (SphereAsset.Succeeded())
    {
        BallMesh->SetStaticMesh(SphereAsset.Object);
        BallMesh->SetRelativeScale3D(FVector(1.42f));
    }

    GlowLight = CreateDefaultSubobject<UPointLightComponent>(TEXT("BallGlow"));
    GlowLight->SetupAttachment(CollisionSphere);
    GlowLight->SetIntensity(9500.0f);
    GlowLight->SetAttenuationRadius(620.0f);
    GlowLight->SetLightColor(FLinearColor(0.08f, 0.65f, 1.0f));
    GlowLight->SetCastShadows(true);

    SpringArm = CreateDefaultSubobject<USpringArmComponent>(TEXT("CameraBoom"));
    SpringArm->SetupAttachment(CollisionSphere);
    SpringArm->TargetArmLength = 620.0f;
    SpringArm->SetRelativeRotation(FRotator(-18.0f, 0.0f, 0.0f));
    SpringArm->bUsePawnControlRotation = false;
    SpringArm->bEnableCameraLag = true;
    SpringArm->CameraLagSpeed = 7.0f;
    SpringArm->bDoCollisionTest = true;

    FollowCamera = CreateDefaultSubobject<UCameraComponent>(TEXT("FollowCamera"));
    FollowCamera->SetupAttachment(SpringArm);
    FollowCamera->bUsePawnControlRotation = false;

    AutoPossessPlayer = EAutoReceiveInput::Player0;
}

void AGlowBallPawn::BeginPlay()
{
    Super::BeginPlay();
    if (APlayerController* PC = Cast<APlayerController>(GetController()))
    {
        PC->SetViewTarget(this);
    }
}

void AGlowBallPawn::Tick(float DeltaTime)
{
    Super::Tick(DeltaTime);
    if (!CollisionSphere) return;

    const FVector Start = CollisionSphere->GetComponentLocation();
    const FVector End = Start - FVector(0.0f, 0.0f, 92.0f);
    FHitResult Hit;
    FCollisionQueryParams Params;
    Params.AddIgnoredActor(this);
    const bool bGrounded = GetWorld()->SweepSingleByChannel(
        Hit, Start, End, FQuat::Identity, ECC_Visibility,
        FCollisionShape::MakeSphere(58.0f), Params);
    if (bGrounded && CollisionSphere->GetPhysicsLinearVelocity().Z <= 35.0f)
    {
        bCanJump = true;
    }
}

void AGlowBallPawn::SetupPlayerInputComponent(UInputComponent* PlayerInputComponent)
{
    Super::SetupPlayerInputComponent(PlayerInputComponent);
    PlayerInputComponent->BindAxis(TEXT("MoveForward"), this, &AGlowBallPawn::MoveForward);
    PlayerInputComponent->BindAxis(TEXT("MoveRight"), this, &AGlowBallPawn::MoveRight);
    PlayerInputComponent->BindAction(TEXT("JumpBall"), IE_Pressed, this, &AGlowBallPawn::JumpPressed);
    PlayerInputComponent->BindAction(TEXT("JumpBall"), IE_Released, this, &AGlowBallPawn::JumpReleased);
}

void AGlowBallPawn::MoveForward(float Value)
{
    if (FMath::Abs(Value) < KINDA_SMALL_NUMBER) return;
    const FRotator YawRotation(0.0f, Controller ? Controller->GetControlRotation().Yaw : 0.0f, 0.0f);
    const FVector Direction = FRotationMatrix(YawRotation).GetUnitAxis(EAxis::X);
    CollisionSphere->AddForce(Direction * Value * DriveForce);
}

void AGlowBallPawn::MoveRight(float Value)
{
    if (FMath::Abs(Value) < KINDA_SMALL_NUMBER) return;
    const FRotator YawRotation(0.0f, Controller ? Controller->GetControlRotation().Yaw : 0.0f, 0.0f);
    const FVector Direction = FRotationMatrix(YawRotation).GetUnitAxis(EAxis::Y);
    CollisionSphere->AddForce(Direction * Value * DriveForce);
}

void AGlowBallPawn::JumpPressed()
{
    if (!bCanJump) return;
    const FVector Start = CollisionSphere->GetComponentLocation();
    FHitResult Hit;
    const FVector End = Start - FVector(0, 0, 95.0f);
    FCollisionQueryParams Params;
    Params.AddIgnoredActor(this);
    if (GetWorld()->SweepSingleByChannel(Hit, Start, End, FQuat::Identity, ECC_Visibility,
        FCollisionShape::MakeSphere(60.0f), Params))
    {
        CollisionSphere->AddImpulse(FVector::UpVector * JumpImpulse);
        bCanJump = false;
    }
}

void AGlowBallPawn::JumpReleased() {}

