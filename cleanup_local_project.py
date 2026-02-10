#!/usr/bin/env python3
"""
Cleanup local project files and create organized structure
"""

import os
import shutil
from pathlib import Path

def cleanup_local_project():
    """Clean up unnecessary files and organize project structure"""
    
    project_root = Path(r"c:\Users\deepd\D\Vibe")
    
    print("=== LOCAL PROJECT CLEANUP ===")
    print(f"Project root: {project_root}")
    
    # Files and directories to remove
    cleanup_items = [
        # Build artifacts and temporary files
        "app/build/",
        "android/app/build/",
        "build/",
        "temp_zip_test/",
        ".gradle/",
        ".idea/",
        "*.iml",
        "local.properties",
        "build_log.txt",
        "build_log_quiet.txt",
        "manifest_log.txt",
        "problems_head.txt",
        
        # Old deployment scripts (keep only latest)
        "vps_deploy_script.py",
        "cleanup_node_modules.py", 
        "fix_domains_no_ports.py",
        "fix_redirects.py",
        "setup_vibe_domains.py",
        "setup_ssl_and_fix_redirects.py",
        "fix_nginx_and_certificates.py",
        "emergency_fix.py",
        "final_status_check.py",
        "diagnose_and_fix_vps.py",
        "fix_port_conflict.py",
        "final_check.py",
        "restart_services.py",
        "test_service.py",
        "cleanup_and_fix.py",
        "fix_ssl_config.py",
        "test_admin_routes.py",
        "update_admin_route.py",
        "setup_database_and_admin.py",
        "fix_admin_panel.py",
        "deploy_admin_files.py",
        "cleanup_and_document.py",
        
        # Redundant configuration files
        "CAPACITOR_INSTRUCTIONS.txt",
        "VIBE_APP_INSTRUCTIONS.txt",
    ]
    
    # Files to keep (important documentation)
    keep_files = [
        "README.md",
        "VIBE_APP_COMPLETE_DOCUMENTATION.md",
        "VIBE_APP_TECHNICAL_SPECIFICATION.md",
        "package.json",
        "package-lock.json",
        "capacitor.config.json",
        "build.gradle",
        "settings.gradle",
        "gradle.properties",
        "gradlew",
        "gradlew.bat"
    ]
    
    print("\n=== REMOVING UNNECESSARY FILES ===")
    
    removed_count = 0
    for item in cleanup_items:
        item_path = project_root / item
        
        # Handle wildcards
        if "*" in item:
            # Remove files matching pattern
            for file_path in project_root.glob(item):
                if file_path.is_file():
                    try:
                        file_path.unlink()
                        print(f"✓ Removed file: {file_path.name}")
                        removed_count += 1
                    except Exception as e:
                        print(f"✗ Failed to remove {file_path.name}: {e}")
        else:
            # Remove directory or file
            if item_path.exists():
                try:
                    if item_path.is_dir():
                        shutil.rmtree(item_path)
                        print(f"✓ Removed directory: {item}")
                    else:
                        item_path.unlink()
                        print(f"✓ Removed file: {item}")
                    removed_count += 1
                except Exception as e:
                    print(f"✗ Failed to remove {item}: {e}")
    
    print(f"\n✓ Removed {removed_count} unnecessary items")
    
    print("\n=== ORGANIZING IMPORTANT FILES ===")
    
    # Create organized directory structure
    directories_to_create = [
        "documentation",
        "android_app",
        "web_frontend", 
        "backend_server",
        "assets",
        "scripts"
    ]
    
    for directory in directories_to_create:
        dir_path = project_root / directory
        if not dir_path.exists():
            dir_path.mkdir()
            print(f"✓ Created directory: {directory}")
    
    # Move documentation files
    doc_files = [
        "README.md",
        "VIBE_APP_COMPLETE_DOCUMENTATION.md",
        "VIBE_APP_TECHNICAL_SPECIFICATION.md"
    ]
    
    for doc_file in doc_files:
        source = project_root / doc_file
        destination = project_root / "documentation" / doc_file
        if source.exists():
            shutil.move(str(source), str(destination))
            print(f"✓ Moved {doc_file} to documentation/")
    
    # Move Android app files
    android_files = [
        "android/",
        "app/"
    ]
    
    for android_file in android_files:
        source = project_root / android_file
        destination = project_root / "android_app" / android_file.rstrip('/')
        if source.exists():
            if source.is_dir():
                shutil.move(str(source), str(destination))
            else:
                shutil.move(str(source), str(project_root / "android_app" / android_file))
            print(f"✓ Moved {android_file} to android_app/")
    
    # Move web frontend files
    web_files = [
        "www/",
        "qr-code.html"
    ]
    
    for web_file in web_files:
        source = project_root / web_file
        destination = project_root / "web_frontend" / web_file.rstrip('/')
        if source.exists():
            if source.is_dir():
                shutil.move(str(source), str(destination))
            else:
                shutil.move(str(source), str(project_root / "web_frontend" / web_file))
            print(f"✓ Moved {web_file} to web_frontend/")
    
    # Move backend server files
    backend_files = [
        "server/"
    ]
    
    for backend_file in backend_files:
        source = project_root / backend_file
        destination = project_root / "backend_server" / backend_file.rstrip('/')
        if source.exists():
            if source.is_dir():
                shutil.move(str(source), str(destination))
            else:
                shutil.move(str(source), str(project_root / "backend_server" / backend_file))
            print(f"✓ Moved {backend_file} to backend_server/")
    
    # Move asset files
    asset_files = [
        "splash-logo.png"
    ]
    
    for asset_file in asset_files:
        # Look for asset files in various locations
        for root, dirs, files in os.walk(project_root):
            if asset_file in files:
                source = Path(root) / asset_file
                destination = project_root / "assets" / asset_file
                shutil.move(str(source), str(destination))
                print(f"✓ Moved {asset_file} to assets/")
                break
    
    # Move important scripts
    script_files = [
        "build-web.bat",
        "install-app.bat",
        "open-app.bat",
        "open-console.bat",
        "open-qr.bat",
        "serve-apk.js",
        "start-all.bat",
        "start-server.bat"
    ]
    
    for script_file in script_files:
        source = project_root / script_file
        destination = project_root / "scripts" / script_file
        if source.exists():
            shutil.move(str(source), str(destination))
            print(f"✓ Moved {script_file} to scripts/")
    
    print("\n=== FINAL PROJECT STRUCTURE ===")
    
    # Show final structure
    final_structure = [
        "documentation/",
        "android_app/",
        "web_frontend/",
        "backend_server/",
        "assets/",
        "scripts/",
        "package.json",
        "package-lock.json",
        "capacitor.config.json",
        "build.gradle",
        "settings.gradle",
        "gradle.properties",
        "gradlew",
        "gradlew.bat"
    ]
    
    for item in final_structure:
        item_path = project_root / item
        if item_path.exists():
            if item_path.is_dir():
                print(f"📁 {item}")
                # Show first few items in directory
                items = list(item_path.iterdir())[:3]
                for subitem in items:
                    print(f"   └── {subitem.name}")
                if len(list(item_path.iterdir())) > 3:
                    print(f"   └── ... and {len(list(item_path.iterdir())) - 3} more items")
            else:
                print(f"📄 {item}")
        else:
            print(f"❌ {item} (missing)")
    
    print("\n" + "="*80)
    print("LOCAL PROJECT CLEANUP COMPLETE!")
    print("="*80)
    print("✓ Removed unnecessary files and directories")
    print("✓ Organized project into logical folders")
    print("✓ Preserved all important documentation")
    print("✓ Maintained essential configuration files")
    print("")
    print("Project is now organized and ready for development!")
    print("="*80)

if __name__ == "__main__":
    print("Starting local project cleanup...")
    cleanup_local_project()