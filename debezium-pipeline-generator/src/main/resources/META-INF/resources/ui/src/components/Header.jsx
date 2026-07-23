import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import {
  AppBar,
  Toolbar,
  Typography,
  Box,
  Button,
  IconButton,
  Menu,
  MenuItem,
  ListItemIcon,
  Avatar,
  Tooltip,
  Drawer,
  List,
  Divider,
  useMediaQuery,
  useTheme,
} from '@mui/material';
import {
  Menu as MenuIcon,
  Dashboard as DashboardIcon,
  Add as AddIcon,
  Settings as SettingsIcon,
  Description as DescriptionIcon,
  CloudUpload as CloudUploadIcon,
  AccountCircle,
  ChevronLeft,
  ChevronRight,
} from '@mui/icons-material';

const menuItems = [
  { path: '/dashboard', label: 'Dashboard', icon: <DashboardIcon /> },
  { path: '/pipelines/new', label: 'Create Pipeline', icon: <AddIcon /> },
  { path: '/settings', label: 'Settings', icon: <SettingsIcon /> },
];

const drawerItems = [
  { path: '/dashboard', label: 'Dashboard', icon: <DashboardIcon /> },
  { path: '/pipelines/new', label: 'Create Pipeline', icon: <AddIcon /> },
  { path: '/settings', label: 'Settings', icon: <SettingsIcon /> },
];

function Header() {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const location = useLocation();
  const [mobileOpen, setMobileOpen] = React.useState(false);
  const [anchorEl, setAnchorEl] = React.useState(null);

  const handleDrawerToggle = () => {
    setMobileOpen(!mobileOpen);
  };

  const handleProfileMenuOpen = (event) => {
    setAnchorEl(event.currentTarget);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
  };

  const handleMobileItemClick = (path) => {
    setMobileOpen(false);
  };

  return (
    <>
      <AppBar position="fixed" elevation={0} sx={{ backgroundColor: '#fff', borderBottom: '1px solid #e0e0e0' }}>
        <Toolbar>
          {isMobile && (
            <IconButton
              color="inherit"
              edge="start"
              onClick={handleDrawerToggle}
              sx={{ mr: 2, color: '#333' }}
            >
              <MenuIcon />
            </IconButton>
          )}

          <Box sx={{ flexGrow: 1, display: 'flex', alignItems: 'center' }}>
            <Link to="/dashboard" style={{ textDecoration: 'none', display: 'flex', alignItems: 'center', gap: 1 }}>
              <Box
                sx={{
                  width: 36,
                  height: 36,
                  borderRadius: 2,
                  background: 'linear-gradient(135deg, #1976d2 0%, #9c27b0 100%)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: 'white',
                  fontWeight: 700,
                  fontSize: '18px',
                }}
              >
                DBZ
              </Box>
              <Typography variant="h6" sx={{ fontWeight: 700, color: '#333' }}>
                Debezium Pipeline Generator
              </Typography>
            </Link>
          </Box>

          {!isMobile && (
            <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
              {menuItems.map((item) => (
                <Button
                  key={item.path}
                  component={Link}
                  to={item.path}
                  variant={location.pathname === item.path ? 'contained' : 'text'}
                  color={location.pathname === item.path ? 'primary' : 'inherit'}
                  startIcon={item.icon}
                  sx={{
                    textTransform: 'none',
                    fontWeight: 500,
                    borderRadius: 2,
                    px: 2,
                  }}
                >
                  {item.label}
                </Button>
              ))}
            </Box>
          )}

          <Tooltip title="Profile">
            <IconButton onClick={handleProfileMenuOpen} sx={{ ml: 1 }}>
              <AccountCircle sx={{ color: '#666' }} />
            </IconButton>
          </Tooltip>

          <Menu
            anchorEl={anchorEl}
            open={Boolean(anchorEl)}
            onClose={handleMenuClose}
            transformOrigin={{ horizontal: 'right', vertical: 'top' }}
            anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
          >
            <MenuItem onClick={handleMenuClose}>
              <ListItemIcon><AccountCircle fontSize="small" /></ListItemIcon>
              Profile
            </MenuItem>
            <MenuItem onClick={handleMenuClose} component={Link} to="/settings">
              <ListItemIcon><SettingsIcon fontSize="small" /></ListItemIcon>
              Settings
            </MenuItem>
            <Divider />
            <MenuItem onClick={handleMenuClose}>
              <ListItemIcon><DescriptionIcon fontSize="small" /></ListItemIcon>
              Documentation
            </MenuItem>
          </Menu>
        </Toolbar>
      </AppBar>

      <Drawer
        variant="temporary"
        open={mobileOpen}
        onClose={handleDrawerToggle}
        ModalProps={{ keepMounted: true }}
        sx={{
          display: { xs: 'block', md: 'none' },
          '& .MuiDrawer-paper': { boxSizing: 'border-box', width: 280 },
        }}
      >
        <Box sx={{ p: 2, borderBottom: 1, borderColor: 'divider' }}>
          <Typography variant="h6" sx={{ fontWeight: 700 }}>
            Debezium Pipeline Generator
          </Typography>
        </Box>
        <List>
          {drawerItems.map((item) => (
            <Button
              key={item.path}
              component={Link}
              to={item.path}
              onClick={() => handleMobileItemClick(item.path)}
              variant={location.pathname === item.path ? 'contained' : 'text'}
              color={location.pathname === item.path ? 'primary' : 'inherit'}
              startIcon={item.icon}
              sx={{
                width: '100%',
                justifyContent: 'flex-start',
                textTransform: 'none',
                mx: 1,
                my: 0.5,
                borderRadius: 2,
              }}
            >
              {item.label}
            </Button>
          ))}
        </List>
        <Divider />
        <Box sx={{ p: 2 }}>
          <Typography variant="caption" color="text.secondary">
            Version 3.7.0-SNAPSHOT
          </Typography>
        </Box>
      </Drawer>
    </>
  );
}

export default Header;